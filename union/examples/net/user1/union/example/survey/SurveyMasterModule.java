package net.user1.union.example.survey;

import net.user1.union.api.Module;
import net.user1.union.core.context.ModuleContext;
import net.user1.union.core.event.RemoteEvent;
import net.user1.union.core.event.RemoteRoomEvent;
import net.user1.union.core.event.RoomEvent;

/**
 * The class that is created when the local room is the MASTER room.
 */
public class SurveyMasterModule implements Module, Runnable {
    private ModuleContext m_ctx;
    
    // the game thread
    private Thread m_gameThread;
    
    // the questions to ask
    private String[] m_questions = {
            "Are you a programmer?",
            "Can you ice skate?",
            "Have you ever been to Australia?"
    };
    
    // results
    private int m_yes;
    private int m_no;
    
    /**
     * Invoked by the server when the room is being created and the module should initialize 
     * itself
     */
    public boolean init(ModuleContext ctx) {
        m_ctx = ctx;
        
        m_gameThread = new Thread(this);
        m_gameThread.start();
        
        // listen for when a client has sent a response (it is sent with a module message)
        m_ctx.getRoom().addEventListener(RoomEvent.MODULE_MESSAGE, this, "onModuleMessage");
        
        // listen for when a new slave room as been added to the cluster so that we can 
        // initialize that slave with the current state of the survey
        m_ctx.getRoom().addRemoteEventListener(RemoteRoomEvent.ADD_SLAVE_ROOM, this, 
                "onAddSlaveRoom");
        
        // listen for when a slave room has sent us the results from clients connected to that
        // node 
        m_ctx.getRoom().addRemoteEventListener("SURVEY_RESULTS", this, "onSurveyResults");
              
        return true;
    }
    
    public void run() {
        int currentQuestion = 0;
        
        while (m_gameThread != null) {
            // ask a question by dispatching a remote event containing the question
            // the event will automatically be sent to all nodes on the cluster
            // and dispatched by the slave instances of this room
            m_ctx.getRoom().dispatchRemoteEvent("ASK_QUESTION", 
                    new AskQuestionEvent(m_questions[currentQuestion]));
            
            // and send to clients connected to our server
            // broadcast the question to the clients connected to this server
            m_ctx.getRoom().sendMessage("QUESTION", m_questions[currentQuestion]);
            
            // advance to the next question
            currentQuestion = (currentQuestion+1) % m_questions.length;
            
            // wait 10 seconds for people to answer
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // then end the question
            m_ctx.getRoom().dispatchRemoteEvent("END_QUESTION", new EndQuestionEvent());
            
            // wait 5 seconds for results to come in
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // output results to System.out and reset results
            System.out.println("Yes: " + m_yes + " No: " + m_no);
            synchronized (this) {
                m_yes = 0;
                m_no = 0;
            }
        }
    }
    
    /**
     * This method is invoked when a module message has been sent by a client to the room. We 
     * use it to get responses from clients that are connected to this server. Responses 
     * collected by clients connected to other servers will be collected by the remote event 
     * "SURVEY_RESULTS".
     */
    public void onModuleMessage(RoomEvent evt) {
        if ("RESPONSE".equals(evt.getMessage().getMessageName())) {
            // add the response to our totals 
            synchronized (this) {
                if ("yes".equals(evt.getMessage().getArg("response"))) {
                    m_yes++;
                } else if ("no".equals(evt.getMessage().getArg("response"))) {
                    m_no++;
                }
            }
        }
    }
    
    /**
     * This method is invoked when a slave room has dispatched its results to the master room.
     */
    public void onSurveyResults(RemoteEvent evt) {
        // get the event Object and increment yes and no 
        SurveyResultsEvent event = (SurveyResultsEvent)evt;
        synchronized (this) {
            m_yes += event.getYes();
            m_no += event.getNo();
        }
    }
    
    /**
     * Invoked by the server when the room is being removed
     */
    public void shutdown() {
        // clean up event listeners
        m_ctx.getRoom().removeEventListener(RoomEvent.MODULE_MESSAGE, this, "onModuleMessage");
        m_ctx.getRoom().removeRemoteEventListener(RemoteRoomEvent.ADD_SLAVE_ROOM, this, 
                "onAddSlaveRoom");
        m_ctx.getRoom().removeRemoteEventListener("SURVEY_RESULTS", this, "onSurveyResults");
        
        m_gameThread = null;
    }
}
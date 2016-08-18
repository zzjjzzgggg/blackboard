package net.user1.union.example.survey;

import net.user1.union.api.Module;
import net.user1.union.core.context.ModuleContext;
import net.user1.union.core.event.RemoteEvent;
import net.user1.union.core.event.RoomEvent;

/**
 * The class that is created when the local room is a SLAVE room.
 */
public class SurveySlaveModule implements Module {
    private ModuleContext m_ctx;
    
    // results
    private int m_yes;
    private int m_no;
    
    public boolean init(ModuleContext ctx) {
        m_ctx = ctx;
        
        // listen for when a client has sent a response (it is sent with a module message)
        m_ctx.getRoom().addEventListener(RoomEvent.MODULE_MESSAGE, this, "onModuleMessage");
        
        // listen for when the master room is asking a question 
        m_ctx.getRoom().addRemoteEventListener("ASK_QUESTION", this, "onAskQuestion");
        
        // listen for when the master room has finished asking a question
        m_ctx.getRoom().addRemoteEventListener("END_QUESTION", this, "onEndQuestion");
        
        return true;
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
     * This method is invoked when the master room is asking a question.
     */
    public void onAskQuestion(RemoteEvent evt) {
        // a custom event so we have to cast it
        AskQuestionEvent event = (AskQuestionEvent)evt;
        
        // broadcast the question to the clients connected to this server
        m_ctx.getRoom().sendMessage("QUESTION", event.getQuestion());
    }

    /**
     * This method is invoked when the master room has finished asking a question.
     */
    public void onEndQuestion(RemoteEvent evt) {
        synchronized (this) {
            // send the results to the master room and reset results
            m_ctx.getRoom().dispatchRemoteEvent("SURVEY_RESULTS", 
                    new SurveyResultsEvent(m_yes, m_no));
            
            m_yes = 0;
            m_no = 0;
        }
    }
    
    public void shutdown() {
        // clean up events
        m_ctx.getRoom().removeEventListener(RoomEvent.MODULE_MESSAGE, this, "onModuleMessage");
        m_ctx.getRoom().removeRemoteEventListener("ASK_QUESTION", this, "onAskQuestion");
        m_ctx.getRoom().removeRemoteEventListener("END_QUESTION", this, "onEndQuestion");
    }
}

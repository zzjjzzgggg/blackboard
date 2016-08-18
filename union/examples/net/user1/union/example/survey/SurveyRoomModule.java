package net.user1.union.example.survey;

import net.user1.union.api.Module;
import net.user1.union.cluster.ClusterRole;
import net.user1.union.core.context.ModuleContext;

/**
 * Module code to add the survey application to a room. This class is designed to work in a 
 * scaled environment and will initialize a master or slave depending on its role within the 
 * cluster. One node in the cluster will have an instance of the room with a ClusterRole of 
 * MASTER (the node where the room was initially created). All other nodes in the cluster will
 * have an instance of the room with a ClusterRole of SLAVE.
 *
 * The survey application asks clients a "yes/no" question and then tallies the results of the 
 * responses. 
 */
public class SurveyRoomModule implements Module {
    private Module m_surveyModule;
    
    public boolean init(ModuleContext ctx) {
        // if it is a slave module then run the slave module code otherwise it is either being 
        // deployed as a master or in a non-clustered environment and so we want the master code 
        // to run
        if (ctx.getRoom().getClusterRole() == ClusterRole.SLAVE) {
            m_surveyModule = new SurveySlaveModule();
        } else {
            m_surveyModule = new SurveyMasterModule();
        }
        
        return m_surveyModule.init(ctx);
    }

    public void shutdown() {
        m_surveyModule.shutdown();
    }
}

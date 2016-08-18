package net.user1.union.example.survey;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.user1.union.core.event.BaseEvent;
import net.user1.union.core.event.RemoteEvent;

/**
 * The SurveyResultsEvent is dispatched by a slave to send results back to the master.
 */
public class SurveyResultsEvent extends BaseEvent implements RemoteEvent, Externalizable {
    private int m_yes;
    private int m_no;
    
    public SurveyResultsEvent(int yes, int no) {
        m_yes = yes;
        m_no = no;
    }
    
    public int getYes() {
        return m_yes;
    }

    public int getNo() {
        return m_no;
    }

    public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
        m_yes = in.readInt();
        m_no = in.readInt();
    }

    public void writeExternal(ObjectOutput out)
    throws IOException {
        out.writeInt(m_yes);
        out.writeInt(m_no);
    }
}

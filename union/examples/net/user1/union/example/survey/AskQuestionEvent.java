package net.user1.union.example.survey;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.user1.union.core.event.BaseEvent;
import net.user1.union.core.event.RemoteEvent;

/**
 * The AskQuestionEvent is dispatched by the master when a new survey question is asked.
 */
public class AskQuestionEvent extends BaseEvent implements RemoteEvent, Externalizable {
    private String m_question;
    
    public AskQuestionEvent(String question) {
        m_question = question;
    }

    public String getQuestion() {
        return m_question;
    }

    public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
        m_question = in.readUTF();
    }

    public void writeExternal(ObjectOutput out)
    throws IOException {
        out.writeUTF(m_question);
    }
}

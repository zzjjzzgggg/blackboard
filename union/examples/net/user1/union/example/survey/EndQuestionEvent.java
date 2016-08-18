package net.user1.union.example.survey;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.user1.union.core.event.BaseEvent;
import net.user1.union.core.event.RemoteEvent;

/**
 * The EndQuestionEvent is dispatched by the master at the end of a survey question.
 */
public class EndQuestionEvent extends BaseEvent implements RemoteEvent, Externalizable {    
    public EndQuestionEvent() {
    }
    
    public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException {
    }

    public void writeExternal(ObjectOutput out)
    throws IOException {
    }
}

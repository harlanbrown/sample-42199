package org.nuxeo.sample.listeners;

import java.util.stream.StreamSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

public class SampleListener implements PostCommitEventListener {
  
    private static final Log log = LogFactory.getLog(SampleListener.class);

    protected void processDoc(DocumentModel doc){
        log.debug("hi");
        if (!doc.isVersion() && doc.hasSchema("picture")){
            // move document
            Framework.doPrivileged(() -> {
                CoreSession session = doc.getCoreSession(); 
                log.debug("moving " + doc.getName());
                session.move(doc.getRef(), new PathRef("/default-domain/workspaces/ws1"), doc.getName());
            });
        }
    }

    @Override
    public void handleEvent(EventBundle events) {
        StreamSupport.stream(events.spliterator(), false)
                     .map(Event::getContext)
                     .filter(DocumentEventContext.class::isInstance)
                     .map(DocumentEventContext.class::cast)
                     .map(DocumentEventContext::getSourceDocument)
                     .filter(doc -> !doc.isVersion() && doc.hasSchema("picture"))
                     .forEach(this::processDoc);

    }
}

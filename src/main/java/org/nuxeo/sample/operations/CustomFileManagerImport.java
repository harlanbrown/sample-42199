/*
 * (C) Copyright 2006-2020 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.sample.operations;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.filemanager.api.FileImporterContext;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.ecm.automation.core.operations.services.FileManagerImport;

/**
 * Use {@link FileManager} to create documents from blobs
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = FileManagerImport.ID, category = Constants.CAT_SERVICES, label = "Create Document from file", description = "Create Document(s) from Blob(s) using the FileManagerService. The destination container must be passed in a Context variable named currentDocument.")
public class CustomFileManagerImport {

    private static final Log log = LogFactory.getLog(CustomFileManagerImport.class);

    public static final String ID = "FileManager.Import";

    protected static final int IMPORT_TX_TIMEOUT_SEC = 86_400; // 1 day

    @Context
    protected CoreSession session;

    @Context
    protected FileManager fileManager;

    @Context
    protected AutomationService as;

    @Context
    protected OperationContext context;

    /** @deprecated since 11.2, use overwrite instead. No more used. */
    @Param(name = "overwite", required = false)
    @Deprecated(since = "11.2")
    protected Boolean overwite = false;

    /** @since 11.2 */
    @Param(name = "overwrite", alias = "overwite", required = false)
    protected Boolean overwrite = false;

    @Param(name = "noMimeTypeCheck", required = false)
    protected Boolean noMimeTypeCheck = false;

    protected DocumentModel getCurrentDocument() throws OperationException {
        String cdRef = (String) context.get("currentDocument");
        return as.getAdaptedValue(context, cdRef, DocumentModel.class);
    }

    @OperationMethod
    public DocumentModel run(Blob blob) throws OperationException, IOException {
        DocumentModel currentDocument = getCurrentDocument();
        String path = currentDocument.getPathAsString();
        FileImporterContext fileCreationContext;

        // check no persist criteria     (same criteria for firing the listener)
        // remember that the doc doesn't exist at this point
        // so the check has to happen on the blob being imported
        if (blob.getMimeType().startsWith("image")){
            fileCreationContext = FileImporterContext.builder(session, blob, path)
                                                     .overwrite(overwrite)
                                                     .mimeTypeCheck(!noMimeTypeCheck)
                                                     .persistDocument(false)
                                                     .build();
        } else {
            fileCreationContext = FileImporterContext.builder(session, blob, path)
                                                     .overwrite(overwrite)
                                                     .mimeTypeCheck(!noMimeTypeCheck)
                                                     .build();
        }

        DocumentModel doc = fileManager.createOrUpdateDocument(fileCreationContext);

        if (doc.isDirty()) {
            doc = doc.getId() == null ? session.createDocument(doc) : session.saveDocument(doc);
        }

        return doc;
    }

    @OperationMethod
    public DocumentModelList run(BlobList blobs) throws OperationException, IOException {
        DocumentModelList result = new DocumentModelListImpl();
        for (Blob blob : blobs) {
            result.add(run(blob));
        }
        return result;
    }

}

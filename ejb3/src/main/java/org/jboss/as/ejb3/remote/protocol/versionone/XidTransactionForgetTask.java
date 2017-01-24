/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.remote.protocol.versionone;

import com.arjuna.ats.internal.jta.transaction.arjunacore.jca.SubordinateTransaction;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.EJBRemoteTransactionsRepository;
import org.jboss.ejb.client.XidTransactionID;
import org.jboss.marshalling.MarshallerFactory;

import javax.transaction.Transaction;
import javax.transaction.xa.XAException;

/**
 * @author Jaikiran Pai
 */
class XidTransactionForgetTask extends XidTransactionManagementTask {

    XidTransactionForgetTask(final TransactionRequestHandler txRequestHandler, final EJBRemoteTransactionsRepository transactionsRepository,
                             final MarshallerFactory marshallerFactory, final XidTransactionID xidTransactionID,
                             final ChannelAssociation channelAssociation, final short invocationId) {
        super(txRequestHandler, transactionsRepository, marshallerFactory, xidTransactionID, channelAssociation, invocationId);
    }

    @Override
    protected void manageTransaction() throws Throwable {
        // first associate the tx on this thread, by resuming the tx
        final SubordinateTransaction transaction = this.transactionsRepository.getImportedTransaction(this.xidTransactionID);
        if(transaction == null) {
            // check the recovery store - it's possible that the "forget" is coming in as part of recovery operation and the subordinate
            // tx may not yet be in memory, but might be in the recovery store
            final Transaction recoveredTransaction = tryRecoveryForImportedTransaction();
            // still not found, so just return
            if (recoveredTransaction == null) {
                if(EjbLogger.EJB3_INVOCATION_LOGGER.isDebugEnabled()) {
                    EjbLogger.EJB3_INVOCATION_LOGGER.debug("Not forgetting " + this.xidTransactionID + " as is was not found on the server");
                }
                return;
            }
        }
        this.resumeTransaction(transaction);

        // "forget"
        try {
            // invoke forget
            transaction.doForget();

        } catch (Exception ex) {
            final XAException xaException = new XAException(XAException.XAER_RMERR);
            xaException.initCause(ex);
            throw xaException;

        } finally {
            transactionsRepository.removeImportedTransaction(xidTransactionID);
            // disassociate the tx that was associated (resumed) on this thread.
            // This needs to be done explicitly because the SubOrdinationManager isn't responsible
            // for clearing the tx context from the thread
            this.transactionsRepository.getTransactionManager().suspend();

        }
    }
}

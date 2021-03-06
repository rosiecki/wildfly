/*
 * Copyright 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.batch;

import org.jberet.repository.JobRepository;
import org.jberet.spi.JobExecutor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.batch.jberet.BatchConfiguration;
import org.wildfly.security.auth.server.SecurityDomain;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class DefaultConfigurationService implements BatchConfiguration, Service<BatchConfiguration> {

    private final InjectedValue<JobExecutor> jobExecutorInjector = new InjectedValue<>();

    @Override
    public boolean isRestartOnResume() {
        return true;
    }

    @Override
    public JobRepository getDefaultJobRepository() {
        // This should never be hit as the job repository is setup in the BatchEnvironmentProcessor
        throw new UnsupportedOperationException();
    }

    @Override
    public JobExecutor getDefaultJobExecutor() {
        return jobExecutorInjector.getValue();
    }

    @Override
    public SecurityDomain getSecurityDomain() {
        // No security domain will be used for the legacy subsystem
        return null;
    }

    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public void stop(final StopContext context) {
    }

    @Override
    public BatchConfiguration getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected InjectedValue<JobExecutor> getJobExecutorInjector() {
        return jobExecutorInjector;
    }
}
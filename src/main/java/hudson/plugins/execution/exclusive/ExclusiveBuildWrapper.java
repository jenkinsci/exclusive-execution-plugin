/**
 * The MIT License
 * 
 * Copyright (c) 2011, Sun Microsystems, Inc., marco.ambu Sam Tavakoli
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.execution.exclusive;

import hudson.Extension;
import hudson.model.Computer;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.execution.exclusive.util.DebugHelper;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author marco.ambu
 * @author Sam Tavakoli
 */
public class ExclusiveBuildWrapper extends BuildWrapper {

    @DataBoundConstructor
    public ExclusiveBuildWrapper(boolean enabled) {
        super();
    }

    @Override
    public BuildWrapper.Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) 
                                                                              throws InterruptedException{
        String nodeName = Computer.currentComputer().getDisplayName();
        
        DebugHelper.info(listener, Messages.ExclusiveBuildWrapper_executingOn() + nodeName);
        DebugHelper.info(listener, Messages.ExclusiveBuildWrapper_shutdownMessage());
        
        try {
            Jenkins.getInstance().doQuietDown();
        } catch (IOException e) {
            DebugHelper.fatalError(listener, Messages.ExclusiveBuildWrapper_errorQuietMode() + 
                                                                                          e.getMessage());
            e.printStackTrace(listener.getLogger());
        }

        while (areComputersIdle(nodeName, Jenkins.getInstance().getComputers()) == false) {
            Thread.sleep(500);
        }
        
        DebugHelper.info(listener, Messages.ExclusiveBuildWrapper_onlyJobRunning());
        return new ExclusiveEnvironment(listener);
    }
    
    /**
     * @param nodeName the name of the node where this job is running from
     * @param computers all computers who has executors available
     * @return true if this job is the only one being executed on this node and all other nodes
     *              are idle. false otherwise
     */
    private boolean areComputersIdle(String nodeName, Computer[] computers){
        for (Computer computer : computers) {
            //any other computer than the one the job is executed on should be idle
            if (computer.getDisplayName().equals(nodeName) == false && computer.isIdle() == false) {
                return false;
            }
            //check if this job is the only one being running on execution computer
            if (computer.getDisplayName().equals(nodeName) == true && computer.countBusy() != 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Descriptor for {@link ExclusiveBuildWrapper}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends BuildWrapperDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.ExclusiveBuildWrapper_DisplayName();
        }

        @Override
        public boolean isApplicable(AbstractProject item) {
            return true;
        }
    }
    
    /**
     * handles the post-build tasks such as canceling the quiet down sequencing 
     * 
     */
    private class ExclusiveEnvironment extends Environment {
        private BuildListener listener;

        public ExclusiveEnvironment(BuildListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean tearDown(AbstractBuild build, BuildListener listener) {
            DebugHelper.info(listener, Messages.ExclusiveBuildWrapper_cancelShutDownMode());
            Jenkins.getInstance().doCancelQuietDown();
            
            return true;
        }
    }
}

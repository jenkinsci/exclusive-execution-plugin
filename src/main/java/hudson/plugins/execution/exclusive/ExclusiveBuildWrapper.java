/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.execution.exclusive;

import hudson.Extension;
import hudson.model.Computer;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import hudson.tasks.BuildWrapperDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

//import hudson.plugins.execution.exclusive.Messages;
import java.io.PrintStream;

/**
 *
 * @author marco.ambu
 */
public class ExclusiveBuildWrapper extends BuildWrapper {
  
  @DataBoundConstructor
  public ExclusiveBuildWrapper(/*boolean enabled*/) {
    super();
  }

  @Override
  public BuildWrapper.Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) {
    final PrintStream logger = listener.getLogger();
    String nodeName = Computer.currentComputer().getDisplayName();
    logger.println("[ExclusiveBuildWrapper] Executing on " + nodeName);
    logger.println("[ExclusiveBuildWrapper] Putting hudson in shutdown mode...");
    hudson.model.Hudson.getInstance().doQuietDown();

    boolean ready = false;
    while (!ready)
    {
      ready = true;
      for(Computer computer: hudson.model.Hudson.getInstance().getComputers())
        if (!nodeName.equals(computer.getDisplayName()) && !computer.isIdle()
            || nodeName.equals(computer.getDisplayName()) && computer.countBusy() != 1)
          ready = false;
      try {
        Thread.sleep(500);
      } catch (InterruptedException ex) {
      }
    }
    logger.println("[ExclusiveBuildWrapper] Only this job is running; starting execution...");
    return new ExclusiveEnvironment(listener);
  }

  class ExclusiveEnvironment extends Environment {

    private BuildListener listener;

    public ExclusiveEnvironment(BuildListener listener) {
      this.listener = listener;
    }

     @Override
     public boolean 	tearDown(AbstractBuild build, BuildListener listener) {
       final PrintStream logger = listener.getLogger();
       logger.println("[ExclusiveBuildWrapper] Canceling hudson shutdown mode...");
       hudson.model.Hudson.getInstance().doCancelQuietDown();
       return true;
     }
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
}

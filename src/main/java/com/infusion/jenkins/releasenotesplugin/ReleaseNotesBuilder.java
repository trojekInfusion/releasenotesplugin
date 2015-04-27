package com.infusion.jenkins.releasenotesplugin;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.infusion.relnotesgen.MainInvoker;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link ReleaseNotesBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author trojek
 */
public class ReleaseNotesBuilder extends Builder {

    public static final CredentialsMatcher CREDENTIALS_MATCHER = CredentialsMatchers.anyOf(new CredentialsMatcher[] {
            CredentialsMatchers.instanceOf(UsernamePasswordCredentialsImpl.class)});
    
    private final String tag1;
    private final String tag2;
    private final String gitDirectory;
    private final String gitBranch;
    private final String gitUrl;
    private final String gitCredentialsId;
    private final String gitCommitterName;
    private final String gitCommitterMail;
    private final String gitCommitMessageValidationOmmiter;
    private final boolean pushReleaseNotes;
    private final String jiraUrl;
    private final String jiraCredentialsId;
    private final String jiraIssuePattern;
    private final String issueFilterByComponent;
    private final String issueFilterByType;
    private final String issueFilterByLabel;
    private final String issueFilterByStatus;
    private final String issueSortType;
    private final String issueSortPriority;
    private final String reportDirectory;
    private final String reportTemplate;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public ReleaseNotesBuilder(final String tag1, final String tag2, final String gitDirectory, final String gitBranch,
            final String gitUrl, final String gitCredentialsId, final String gitCommitterName,
            final String gitCommitterMail, final String gitCommitMessageValidationOmmiter,
            final boolean pushReleaseNotes, final String jiraUrl, final String jiraCredentialsId,
            final String jiraIssuePattern, final String issueFilterByComponent, final String issueFilterByType,
            final String issueFilterByLabel, final String issueFilterByStatus, final String issueSortType,
            final String issueSortPriority, final String reportDirectory, final String reportTemplate) {
        super();
        this.tag1 = tag1;
        this.tag2 = tag2;
        this.gitDirectory = gitDirectory;
        this.gitBranch = gitBranch;
        this.gitUrl = gitUrl;
        this.gitCredentialsId = gitCredentialsId;
        this.gitCommitterName = gitCommitterName;
        this.gitCommitterMail = gitCommitterMail;
        this.gitCommitMessageValidationOmmiter = gitCommitMessageValidationOmmiter;
        this.pushReleaseNotes = pushReleaseNotes;
        this.jiraUrl = jiraUrl;
        this.jiraCredentialsId = jiraCredentialsId;
        this.jiraIssuePattern = jiraIssuePattern;
        this.issueFilterByComponent = issueFilterByComponent;
        this.issueFilterByType = issueFilterByType;
        this.issueFilterByLabel = issueFilterByLabel;
        this.issueFilterByStatus = issueFilterByStatus;
        this.issueSortType = issueSortType;
        this.issueSortPriority = issueSortPriority;
        this.reportDirectory = reportDirectory;
        this.reportTemplate = reportTemplate;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getTag1() {
        return tag1;
    }

    public String getTag2() {
        return tag2;
    }

    public String getGitDirectory() {
        return gitDirectory;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public String getGitCredentialsId() {
        return gitCredentialsId;
    }

    public String getGitCommitterName() {
        return gitCommitterName;
    }

    public String getGitCommitterMail() {
        return gitCommitterMail;
    }

    public String getGitCommitMessageValidationOmmiter() {
        return gitCommitMessageValidationOmmiter;
    }

    public boolean isPushReleaseNotes() {
        return pushReleaseNotes;
    }

    public String getJiraUrl() {
        return jiraUrl;
    }

    public String getJiraCredentialsId() {
        return jiraCredentialsId;
    }

    public String getJiraIssuePattern() {
        return jiraIssuePattern;
    }

    public String getIssueFilterByComponent() {
        return issueFilterByComponent;
    }

    public String getIssueFilterByType() {
        return issueFilterByType;
    }

    public String getIssueFilterByLabel() {
        return issueFilterByLabel;
    }

    public String getIssueFilterByStatus() {
        return issueFilterByStatus;
    }

    public String getIssueSortType() {
        return issueSortType;
    }

    public String getIssueSortPriority() {
        return issueSortPriority;
    }

    public String getReportDirectory() {
        return reportDirectory;
    }

    public String getReportTemplate() {
        return reportTemplate;
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        // This is where you 'build' the project.

        if (getDescriptor().getUseGit()) {
            try {
                StandardUsernamePasswordCredentials gitUsernamePassword = CredentialsProvider.findCredentialById(gitCredentialsId, UsernamePasswordCredentialsImpl.class, build);
                StandardUsernamePasswordCredentials jiraUsernamePassword = CredentialsProvider.findCredentialById(jiraCredentialsId, UsernamePasswordCredentialsImpl.class, build);
                listener.getLogger().println("Founded git credentials " + gitUsernamePassword.getDescription());
                listener.getLogger().println("Founded jira credentials " + jiraUsernamePassword.getDescription());
                new MainInvoker()
                    .tagStart(tag1)
                    .tagEnd(tag2)
                    .pushReleaseNotes(pushReleaseNotes)
                    .gitDirectory(gitDirectory)
                    .gitBranch(gitBranch)
                    .gitUrl(gitUrl)
                    .gitUsername(gitUsernamePassword.getUsername())
                    .gitPassword(gitUsernamePassword.getPassword().getPlainText())
                    .gitCommitterName(gitCommitterName)
                    .gitCommitterMail(gitCommitterMail)
                    .gitCommitMessageValidationOmmiter(gitCommitMessageValidationOmmiter)
                    .jiraUrl(jiraUrl)
                    .jiraUsername(jiraUsernamePassword.getUsername())
                    .jiraPassword(jiraUsernamePassword.getPassword().getPlainText())
                    .jiraIssuePattern(jiraIssuePattern)
                    .issueFilterByComponent(issueFilterByComponent)
                    .issueFilterByType(issueFilterByType)
                    .issueFilterByLabel(issueFilterByLabel)
                    .issueFilterByStatus(issueFilterByStatus)
                    .issueSortType(issueSortType)
                    .issueSortPriority(issueSortPriority)
                    .reportDirectory(reportDirectory)
                    .reportTemplate(reportTemplate)
                    .invoke();
            } catch (Exception e) {
                listener.getLogger().println(e.getMessage());
            }

        } else {
            listener.getLogger().println("We're very sorry but for now you can use only git as scm provider.");
        }
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link ReleaseNotesBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useGit;

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user.
         */
        public FormValidation doCheckGitDirectory(@QueryParameter final String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set git directory");
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Release notes plugin";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useGit = formData.getBoolean("useGit");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseGit() {
            return useGit;
        }

        public ListBoxModel doFillGitCredentialsIdItems(@AncestorInPath final Item project) {
            if (project == null || !project.hasPermission(Item.CONFIGURE)) {
                return new StandardListBoxModel();
            }
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withMatching(
                            CREDENTIALS_MATCHER,
                            CredentialsProvider.lookupCredentials(StandardCredentials.class,
                                    project,
                                    ACL.SYSTEM)
                    );
        }

        public ListBoxModel doFillJiraCredentialsIdItems(@AncestorInPath final Item project) {
            return doFillGitCredentialsIdItems(project);
        }
    }
}


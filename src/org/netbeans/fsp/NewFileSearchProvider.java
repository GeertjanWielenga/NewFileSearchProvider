package org.netbeans.fsp;

import java.awt.Component;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.project.uiapi.ProjectChooserFactory;
import org.netbeans.spi.java.project.support.ui.templates.JavaTemplates;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

public class NewFileSearchProvider implements SearchProvider {

    @Override
    public void evaluate(SearchRequest request, SearchResponse response) {

        for (final FileObject item : FileUtil.getOrder(Arrays.asList(getAllItemsToSearchIn().getChildren()), true)) {
            if (isConditionSatisfied(item)) {
                if (!response.addResult(new Runnable() {
                    @Override
                    public void run() {
                        Project project = Utilities.actionsGlobalContext().lookup(Project.class);
                        if (project == null) {
                            FileObject fo = Utilities.actionsGlobalContext().lookup(FileObject.class);
                            if (fo == null) {
                                JOptionPane.showMessageDialog(null, "Select a project or file...");
                            } else {
                                project = FileOwnerQuery.getOwner(fo);
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Select a project or file...");
                        }
                        if (project != null) {
                            Sources sources = (Sources) ProjectUtils.getSources(project);
                            SourceGroup[] groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
                            List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<WizardDescriptor.Panel<WizardDescriptor>>();
                            panels.add(JavaTemplates.createPackageChooser(project, groups));
                            String[] steps = new String[panels.size()];
                            for (int i = 0; i < panels.size(); i++) {
                                Component c = panels.get(i).getComponent();
                                steps[i] = c.getName();
                                if (c instanceof JComponent) {
                                    JComponent jc = (JComponent) c;
                                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                                    jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, false);
                                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
                                }
                            }
                            WizardDescriptor wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<WizardDescriptor>(panels));
                            wiz.putProperty(ProjectChooserFactory.WIZARD_KEY_TEMPLATE, item);
                            wiz.setTitleFormat(new MessageFormat("{0}"));
                            wiz.setTitle("New " + item.getNameExt());
                            if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
                                FileObject dir = Templates.getTargetFolder(wiz);
                                DataFolder df = DataFolder.findFolder(dir);
                                FileObject template = Templates.getTemplate(wiz);
                                try {
                                    DataObject dTemplate = DataObject.find(template);
                                    DataObject object = dTemplate.createFromTemplate(df, Templates.getTargetName(wiz));
                                    object.getLookup().lookup(OpenCookie.class).open();
                                } catch (DataObjectNotFoundException ex) {
                                    Exceptions.printStackTrace(ex);
                                } catch (IOException ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                            }
                        }
                    }
                }, item.getNameExt())) {
                    break;
                }
            }
        }

    }

    private FileObject getAllItemsToSearchIn() {
        return FileUtil.getConfigFile("Templates/Classes");
    }

    private boolean isConditionSatisfied(FileObject item) {
        if (item.getName().equals("Code") || item.getName().equals("Package") || item.getName().equals("package-info")) {
            return false;
        } else {
            return true;
        }
    }

}

package apps.startup;

import apps.StartupModel;
import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import jmri.script.ScriptFileChooser;

/**
 *
 * @author Randall Wood 2016
 */
public class ScriptButtonModelFactory implements StartupModelFactory {

    private ScriptFileChooser chooser = null;
    
    public ScriptButtonModelFactory() {
    }

    @Override
    public Class<? extends StartupModel> getModelClass() {
        return ScriptButtonModel.class;
    }

    @Override
    public ScriptButtonModel newModel() {
        return new ScriptButtonModel();
    }

    @Override
    public String getDescription() {
        return Bundle.getMessage(this.getModelClass().getCanonicalName());
    }

    @Override
    public String getActionText() {
        return Bundle.getMessage("EditableStartupAction", this.getDescription()); // NOI18N
    }

    @Override
    public void editModel(StartupModel model, Component parent) {
        if (this.chooser == null) {
            this.chooser = new ScriptFileChooser();
            this.chooser.setDialogTitle(this.getDescription());
            this.chooser.setDialogType(JFileChooser.CUSTOM_DIALOG);
        }
        if (ScriptButtonModel.class.isInstance(model)) {
            ScriptButtonPanel buttonPanel = new ScriptButtonPanel(this.chooser, parent);
            if (((ScriptButtonModel) model).getScript() != null) {
                buttonPanel.setScript(((ScriptButtonModel) model).getScript().getPath());
            } else {
                buttonPanel.setScript(""); // NOI18N
            }
            buttonPanel.setButtonName(model.getName());
            JOptionPane.showMessageDialog(parent,
                    buttonPanel,
                    this.getDescription(),
                    JOptionPane.PLAIN_MESSAGE);
            if (!buttonPanel.getButtonName().isEmpty()) {
                model.setName(buttonPanel.getButtonName());
                ((ScriptButtonModel) model).setScript(new File(buttonPanel.getScript()));
            }
        }
    }

    @Override
    public void initialize() {
        // nothing to do
    }

}

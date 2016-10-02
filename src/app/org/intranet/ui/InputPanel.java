/*
 * Copyright 2003, 2005 Neil McKellar and Chris Dailey
 * All rights reserved.
 */
package org.intranet.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * @author Neil McKellar and Chris Dailey
 *
 */
public abstract class InputPanel
  extends JPanel
{
  protected JPanel center = new JPanel(new GridBagLayout());
  protected int centerRow = 1; // skip the header row
  protected MemberArrays members = new MemberArrays();
  
  private JProgressBar progressBar;
  private JLabel progressMessage;
  private JButton applyButton;

  protected class MemberArrays
  {
    private List<JComponent> baseInputFields = new ArrayList<JComponent>();
    private List<JTextField> maxInputFields = new ArrayList<JTextField>();
    private List<JTextField> incrementInputFields = new ArrayList<JTextField>();
    private List<JCheckBox> checkboxInputFields = new ArrayList<JCheckBox>();
    private List<Parameter> inputParams = new ArrayList<Parameter>();

    void addStuffToArrays(Parameter p, JComponent base, JTextField max,
        JTextField increment, JCheckBox checkbox)
    {
      baseInputFields.add(base);
      maxInputFields.add(max);
      incrementInputFields.add(increment);
      checkboxInputFields.add(checkbox);
      inputParams.add(p);
    }

    void addStuffToArrays(Parameter p, JComponent base)
    {
      addStuffToArrays(p, base, null, null, null);
    }
    int getSize() { return inputParams.size(); }
    Parameter getParameter(int i) { return inputParams.get(i); }
    List getParameters() { return inputParams; }
    JComponent getBaseInputField(int i)
    { return baseInputFields.get(i); }
    JTextField getMaxInputField(int i)
    { return maxInputFields.get(i); }
    JTextField getIncrementInputField(int i)
    { return incrementInputFields.get(i); }
    JCheckBox getCheckboxInputField(int i)
    { return checkboxInputFields.get(i); }

    private void copyUIToParameters()
    {
      for (int i = 0; i < getSize(); i++)
      {
        JComponent field = getBaseInputField(i);
        Parameter param = getParameter(i);
        copyUIToParameter(i, field, param);
      }
    }
  }

  protected abstract void copyUIToParameter(int memberIndex, JComponent field,
    Parameter param);

  private List<Listener> listeners = new ArrayList<Listener>();

  public interface Listener
  {
    void parametersApplied();
  }

  public void addListener(Listener l)
  {
	if (l != null)
      listeners.add(l);
  }

  public void removeListener(Listener l)
  {
    listeners.remove(l);
  }
  
  private InputPanel()
  {
    super();
    setLayout(new BorderLayout());
    JPanel centered = new JPanel(new FlowLayout(FlowLayout.CENTER));
    applyButton = new JButton("Apply");
    centered.add(applyButton);
    progressBar = new JProgressBar();
    progressBar.setIndeterminate(true);
    progressBar.setVisible(false);
    progressMessage = new JLabel();
    progressMessage.setVisible(false);
    JPanel progressContainer = new JPanel();
    progressContainer.setLayout(new BoxLayout(progressContainer, BoxLayout.Y_AXIS));
    progressContainer.add(progressMessage);
    progressContainer.add(progressBar);
    centered.add(progressContainer);
    add(centered, BorderLayout.SOUTH);
    applyButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ae)
      {
        members.copyUIToParameters();
        try
        {
          applyParameters();
        }
        catch (Exception e)
        {
          Window window = SwingUtilities.windowForComponent(InputPanel.this);
          new ExceptionDialog(window, members.getParameters(), e);
        }
      }
    });
    
    add(center, BorderLayout.CENTER);
  }
  
  public void showIndeterminateProgress(String message)
  {
	  applyButton.setVisible(false);
	  progressBar.setVisible(true);
	  progressMessage.setText(message);
	  progressMessage.setVisible(true);
  }

  public void hideIndeterminateProgress()
  {
	  progressBar.setVisible(false);
	  progressMessage.setVisible(false);
	  applyButton.setVisible(true);
  }
  
  protected InputPanel(List<? extends Parameter> parameters, Listener l)
  {
    this();

    for (Parameter p : parameters)
      addParameter(p);
    addListener(l);
  }

  protected abstract void addParameter(Parameter p);
  
  public void applyParameters()
  {
    for (Listener l : listeners)
      l.parametersApplied();
  }
}

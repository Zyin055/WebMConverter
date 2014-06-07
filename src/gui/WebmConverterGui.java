package gui;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;
import slider.RangeSlider;
import slider.RangeSliderUI;

public class WebmConverterGui
{
	public static final String VERSION = "1.0.0";
	public static final String RELEASE_DATE = "6/06/2014";
	
	private Preferences prefs = Preferences.userRoot().node(getClass().getName());
	
	private boolean DISABLE_SLIDER_CHANGE_EVENTS = false;
	
	private Process p;

	private boolean rangeSliderIsLowerDragging = false;
	private boolean rangeSliderIsUpperDragging = false;
	
	private JFrame frmWebmConverter;
    private JMenuBar menuBar;
	private JTextField inputFileTextField;
	private JTextField outputFileTextField;
	private JTextField textFieldMaxFileSize;
	private JLabel lblNudgeStartLabel;
	private JLabel lblNudgeEndLabel;
	private JLabel lblPreviewStart;
	private JLabel lblPreviewEnd;
    private JLabel lblStartLabel;
    private JLabel lblEndLabel;
    private JLabel lblProgressBar;
    private JButton btnMakeWebm;
    private JCheckBox chckbxPlayWhenDone;
    private JSpinner spinnerThreads;
    private JSpinner spinnerCpuUsed;
    private JSpinner spinnerFrameRate;
    private JSpinner spinnerBitrate;
    private JSpinner spinnerScale;
    private JCheckBox chckbxNoAudio;
    private JCheckBox chckbxOverwrite;
	private JFileChooser fc;
    private RangeSlider rangeSlider;

    private int rangeSliderStartTime = 0;
    private int rangeSliderEndTime = 0;

    private double nudgeStartTime = 0f;
    private double nudgeEndTime = 0f;
    
    private boolean GENERATING_WEBM = false;
    private JSpinner spinnerQMax;
    private JProgressBar progressBar;
    private JButton btnCancel;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					WebmConverterGui window = new WebmConverterGui();
					window.frmWebmConverter.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public WebmConverterGui() {
		initialize();
	}
	private void initialize() {
		frmWebmConverter = new JFrame();
		ImageIcon icon = new ImageIcon(WebmConverterGui.class.getResource("/img/WebM-Logo.png"));
		frmWebmConverter.setIconImage(icon.getImage());
		frmWebmConverter.setTitle("WebM Converter");
		frmWebmConverter.setBounds(100, 100, 870, 610);
		frmWebmConverter.setResizable(false);
		frmWebmConverter.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmWebmConverter.getContentPane().setLayout(new BorderLayout(0, 0));
		frmWebmConverter.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                SavePreferences();
                DeleteScreenshotFiles();
            }
        });
		
		
		//instantiate the file chooser at the last opened folder
        fc = new JFileChooser(prefs.get("LastUsedFolder", new File(".").getAbsolutePath()));

        rangeSlider = new RangeSlider();
        rangeSlider.setPreferredSize(new Dimension(850, 12));
        rangeSlider.setMinimum(0);
        rangeSlider.setMaximum(10);
        rangeSlider.setUpperValue(10);
        rangeSlider.setValue(0);
        
        // Add listener to update display.
        rangeSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
            	if(!DISABLE_SLIDER_CHANGE_EVENTS)
            	{
                    RangeSlider slider = (RangeSlider) e.getSource();

                    if(!slider.getValueIsAdjusting())
                    {
                        if(rangeSliderIsLowerDragging)
                        	RangeSliderLower_Changed(slider);
                        if(rangeSliderIsUpperDragging)
                        	RangeSliderUpper_Changed(slider);
                    }
                    
                    rangeSliderIsLowerDragging = ((RangeSliderUI)slider.getUI()).lowerDragging;
                    rangeSliderIsUpperDragging = ((RangeSliderUI)slider.getUI()).upperDragging;

                    if(rangeSliderIsLowerDragging)
                    	RangeSliderLower_Changing(slider);
                    if(rangeSliderIsUpperDragging)
                    	RangeSliderUpper_Changing(slider);
            	}
            }
        });
		
		menuBar = new JMenuBar();
		frmWebmConverter.getContentPane().add(menuBar, BorderLayout.NORTH);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmClose = new JMenuItem("Close");
		mntmClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileMenu_File_Close_Clicked();
			}
		});
		
		JMenuItem mntmOpen = new JMenuItem("Open...");
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileMenu_Open_Clicked();
			}
		});
		mnFile.add(mntmOpen);
		mnFile.add(mntmClose);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmHowDoI = new JMenuItem("How do I use this?");
		mntmHowDoI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileMenu_Help_HowDoIUseThis_Clicked();
			}
		});
		mnHelp.add(mntmHowDoI);
		
		JPanel panel = new JPanel();
		frmWebmConverter.getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new MigLayout("wrap 1", "[]", "[][][][][][]"));
		
		JPanel panelInput = new JPanel();
		panelInput.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(panelInput, "cell 0 0,growx,aligny center");
		
		JButton btnInputFile = new JButton("Input Video...");
		btnInputFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				InputFile_Clicked();
			}
		});
		
		panelInput.setLayout(new MigLayout("fillx", "[][grow]", "[][]"));
		panelInput.add(btnInputFile, "alignx center");
		
		inputFileTextField = new JTextField();
		inputFileTextField.setEditable(false);
		panelInput.add(inputFileTextField, "flowx,growx,wrap");
		inputFileTextField.setColumns(10);
		
		JLabel lblOutputFileName = new JLabel("Output file name:");
		panelInput.add(lblOutputFileName, "alignx center");
		
		outputFileTextField = new JTextField();
		panelInput.add(outputFileTextField, "flowx,growx");
		outputFileTextField.setColumns(10);
		
		chckbxOverwrite = new JCheckBox("Overwrite");
		chckbxOverwrite.setSelected(true);
		panelInput.add(chckbxOverwrite, "cell 1 1");
		
		
		JPanel panelOptions = new JPanel();
		panelOptions.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(panelOptions, "cell 0 1,growx,aligny center");
		panelOptions.setLayout(new MigLayout("", "[grow]", "[grow]"));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		panelOptions.add(tabbedPane, "cell 0 0,grow");
		
		JPanel panelTabQuality = new JPanel();
		tabbedPane.addTab("Quality", null, panelTabQuality, null);
		panelTabQuality.setLayout(new MigLayout("", "[][]10[][]10[][]10[][]", "[20px]"));
		
		JLabel lblCpuUsed = new JLabel("Encoding speed");
		panelTabQuality.add(lblCpuUsed, "cell 0 0,alignx left,aligny center");
		lblCpuUsed.setToolTipText("Higher values GREATLY speeds up the encoding process, but will lower the quality of the video. Final products should use a value of 1. While testing you should use a value of 5. Valid range is 1-5. Default is 5.");
		
		spinnerCpuUsed = new JSpinner();
		panelTabQuality.add(spinnerCpuUsed, "cell 1 0,alignx left,aligny top");
		spinnerCpuUsed.setModel(new SpinnerNumberModel(5, 1, 5, 1));
		
		JLabel lblFramerate = new JLabel("Frame rate");
		panelTabQuality.add(lblFramerate, "cell 2 0,alignx left,aligny center");
		lblFramerate.setToolTipText("Output frame rate of your WebM. Default frame rate for most videos is 24.");
		
		spinnerFrameRate = new JSpinner();
		panelTabQuality.add(spinnerFrameRate, "cell 3 0,alignx left,aligny top");
		spinnerFrameRate.setToolTipText("Default: 24");
		spinnerFrameRate.setModel(new SpinnerNumberModel(24, 1, 120, 1));
		
		JLabel lblBitrate = new JLabel("Bitrate (kb)");
		panelTabQuality.add(lblBitrate, "cell 4 0,alignx left,aligny center");
		lblBitrate.setToolTipText("Having a higher bitrate will potentially produce better quality videos. Default: 64 kb/s.");
		
		spinnerBitrate = new JSpinner();
		panelTabQuality.add(spinnerBitrate, "cell 5 0,alignx left,aligny top");
		spinnerBitrate.setModel(new SpinnerNumberModel(64, 1, 2048, 1));
		
		JLabel lblCompression = new JLabel("Compression");
		lblCompression.setToolTipText("Set the max compression level. Higher values means a more compressed video with less quality. Compression has the largest overall impact on the quality/size of the output WebM. Valid range is 4-51.");
		panelTabQuality.add(lblCompression, "cell 6 0,alignx left,aligny center");
		
		spinnerQMax = new JSpinner();
		spinnerQMax.setModel(new SpinnerNumberModel(40, 4, 51, 1));
		panelTabQuality.add(spinnerQMax, "cell 7 0,alignx left,aligny top");
		
		JPanel panelTabGeneral = new JPanel();
		tabbedPane.addTab("General", null, panelTabGeneral, null);
		panelTabGeneral.setLayout(new MigLayout("", "[]10[][]10[][]10[][]", "[23px]"));
		
		chckbxNoAudio = new JCheckBox("No Audio");
		panelTabGeneral.add(chckbxNoAudio, "cell 0 0,alignx left,aligny top");
		chckbxNoAudio.setToolTipText("Some imageboards does not accept WebMs with audio.");
		chckbxNoAudio.setSelected(true);
		
		JLabel lblMaxFileSize = new JLabel("Max file size (bytes)");
		panelTabGeneral.add(lblMaxFileSize, "cell 1 0,alignx left,aligny center");
		lblMaxFileSize.setToolTipText("Anything higher than 3 megabytes can't be uploaded to most imageboards. If your WebM is being cut off before reaching the end time you can set this limit higher.");
		
		textFieldMaxFileSize = new JTextField();
		panelTabGeneral.add(textFieldMaxFileSize, "cell 2 0,alignx left,aligny center");
		textFieldMaxFileSize.setText("3000000");
		textFieldMaxFileSize.setColumns(10);
		
		JLabel lblCpuThreads = new JLabel("CPU Threads");
		panelTabGeneral.add(lblCpuThreads, "cell 3 0,alignx left,aligny center");
		lblCpuThreads.setToolTipText("How many CPU threads should be created to make your WebM. Should only be set as high as the number of cores your CPU has.");
		
		spinnerThreads = new JSpinner();
		panelTabGeneral.add(spinnerThreads, "cell 4 0,alignx left,aligny center");
		spinnerThreads.setModel(new SpinnerNumberModel(new Integer(4), new Integer(1), null, new Integer(1)));
		
		JLabel lblScaleX = new JLabel("Scale  %");
		panelTabGeneral.add(lblScaleX, "cell 5 0,alignx left,aligny center");
		lblScaleX.setToolTipText("Scale the output WebM to be this big compared to the original.");
		
		spinnerScale = new JSpinner();
		panelTabGeneral.add(spinnerScale, "cell 6 0,alignx left,aligny center");
		spinnerScale.setModel(new SpinnerNumberModel(100, 1, 1000, 1));
		
		JPanel panelScreenshots = new JPanel();
		panelScreenshots.setPreferredSize(new Dimension(600, 215));
		FlowLayout fl_panelScreenshots = (FlowLayout) panelScreenshots.getLayout();
		fl_panelScreenshots.setVgap(0);
		fl_panelScreenshots.setHgap(10);
		panel.add(panelScreenshots, "cell 0 2,grow");
		
		lblPreviewStart = new JLabel("");
		panelScreenshots.add(lblPreviewStart);
		
		lblPreviewEnd = new JLabel("");
		panelScreenshots.add(lblPreviewEnd);
		
		JPanel panelScreenshotControls = new JPanel();
		panelScreenshotControls.setAlignmentY(Component.TOP_ALIGNMENT);
		panel.add(panelScreenshotControls, "cell 0 3,growx,aligny top");
		GridBagLayout gbl_panelScreenshotControls = new GridBagLayout();
		gbl_panelScreenshotControls.columnWidths = new int[] {50, 50, 250, 250, 50, 50};
		gbl_panelScreenshotControls.rowHeights = new int[] {0, 0, 0, 0};
		gbl_panelScreenshotControls.columnWeights = new double[]{0.0, 0.0, 1.0, 1.0, 0.0, 0.0};
		gbl_panelScreenshotControls.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		panelScreenshotControls.setLayout(gbl_panelScreenshotControls);
		
		GridBagConstraints gbc_lblSliderGoesHere = new GridBagConstraints();
		gbc_lblSliderGoesHere.gridwidth = 6;
		gbc_lblSliderGoesHere.insets = new Insets(0, 0, 5, 0);
		gbc_lblSliderGoesHere.gridx = 0;
		gbc_lblSliderGoesHere.gridy = 0;
		panelScreenshotControls.add(rangeSlider, gbc_lblSliderGoesHere);
		
		JButton buttonNudgeLeftStartLarge = new JButton("<<");
		buttonNudgeLeftStartLarge.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LargeNudgeButtons_Clicked(true, -1);
			}
		});
		GridBagConstraints gbc_buttonNudgeLeftStartLarge = new GridBagConstraints();
		gbc_buttonNudgeLeftStartLarge.insets = new Insets(0, 0, 5, 5);
		gbc_buttonNudgeLeftStartLarge.gridx = 0;
		gbc_buttonNudgeLeftStartLarge.gridy = 1;
		panelScreenshotControls.add(buttonNudgeLeftStartLarge, gbc_buttonNudgeLeftStartLarge);
		
		JButton buttonNudgeLeftRightLarge = new JButton(">>");
		buttonNudgeLeftRightLarge.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LargeNudgeButtons_Clicked(true, 1);
			}
		});
		GridBagConstraints gbc_buttonNudgeLeftRightLarge = new GridBagConstraints();
		gbc_buttonNudgeLeftRightLarge.insets = new Insets(0, 0, 5, 5);
		gbc_buttonNudgeLeftRightLarge.gridx = 1;
		gbc_buttonNudgeLeftRightLarge.gridy = 1;
		panelScreenshotControls.add(buttonNudgeLeftRightLarge, gbc_buttonNudgeLeftRightLarge);
		
		lblStartLabel = new JLabel("00:00:00");
		GridBagConstraints gbc_lblStartLabel = new GridBagConstraints();
		gbc_lblStartLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblStartLabel.gridx = 2;
		gbc_lblStartLabel.gridy = 1;
		panelScreenshotControls.add(lblStartLabel, gbc_lblStartLabel);
		
		
		
		JButton buttonNudgeLeftStart = new JButton("<");
		buttonNudgeLeftStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NudgeButtons_Click(true, -0.02f);
			}
		});
		
		JButton buttonNudgeRightEndLarge = new JButton(">>");
		buttonNudgeRightEndLarge.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LargeNudgeButtons_Clicked(false, 1);
			}
		});
		
		JButton buttonNudgeLeftEndLarge = new JButton("<<");
		buttonNudgeLeftEndLarge.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LargeNudgeButtons_Clicked(false, -1);
			}
		});
		
		lblEndLabel = new JLabel("00:00:00");
		GridBagConstraints gbc_lblEndLabel = new GridBagConstraints();
		gbc_lblEndLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblEndLabel.gridx = 3;
		gbc_lblEndLabel.gridy = 1;
		panelScreenshotControls.add(lblEndLabel, gbc_lblEndLabel);
		GridBagConstraints gbc_buttonNudgeLeftEndLarge = new GridBagConstraints();
		gbc_buttonNudgeLeftEndLarge.insets = new Insets(0, 0, 5, 5);
		gbc_buttonNudgeLeftEndLarge.gridx = 4;
		gbc_buttonNudgeLeftEndLarge.gridy = 1;
		panelScreenshotControls.add(buttonNudgeLeftEndLarge, gbc_buttonNudgeLeftEndLarge);
		GridBagConstraints gbc_buttonNudgeRightEndLarge = new GridBagConstraints();
		gbc_buttonNudgeRightEndLarge.insets = new Insets(0, 0, 5, 0);
		gbc_buttonNudgeRightEndLarge.gridx = 5;
		gbc_buttonNudgeRightEndLarge.gridy = 1;
		panelScreenshotControls.add(buttonNudgeRightEndLarge, gbc_buttonNudgeRightEndLarge);
		GridBagConstraints gbc_buttonNudgeLeftStart = new GridBagConstraints();
		gbc_buttonNudgeLeftStart.anchor = GridBagConstraints.NORTH;
		gbc_buttonNudgeLeftStart.insets = new Insets(0, 0, 0, 5);
		gbc_buttonNudgeLeftStart.gridx = 0;
		gbc_buttonNudgeLeftStart.gridy = 2;
		panelScreenshotControls.add(buttonNudgeLeftStart, gbc_buttonNudgeLeftStart);
		
		JButton buttonNudgeRightStart = new JButton(">");
		buttonNudgeRightStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NudgeButtons_Click(true, 0.02f);
			}
		});
		GridBagConstraints gbc_buttonNudgeRightStart = new GridBagConstraints();
		gbc_buttonNudgeRightStart.anchor = GridBagConstraints.NORTH;
		gbc_buttonNudgeRightStart.insets = new Insets(0, 0, 0, 5);
		gbc_buttonNudgeRightStart.gridx = 1;
		gbc_buttonNudgeRightStart.gridy = 2;
		panelScreenshotControls.add(buttonNudgeRightStart, gbc_buttonNudgeRightStart);
		
		lblNudgeStartLabel = new JLabel("+0.00s");
		GridBagConstraints gbc_lblNudgeStartLabel = new GridBagConstraints();
		gbc_lblNudgeStartLabel.insets = new Insets(0, 0, 0, 5);
		gbc_lblNudgeStartLabel.gridx = 2;
		gbc_lblNudgeStartLabel.gridy = 2;
		panelScreenshotControls.add(lblNudgeStartLabel, gbc_lblNudgeStartLabel);
		
		JButton buttonNudgeRightEnd = new JButton(">");
		buttonNudgeRightEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				NudgeButtons_Click(false, 0.02f);
			}
		});
		
		JButton buttonNudgeLeftEnd = new JButton("<");
		buttonNudgeLeftEnd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				NudgeButtons_Click(false, -0.02f);
			}
		});
		
		lblNudgeEndLabel = new JLabel("+0.00s");
		GridBagConstraints gbc_lblNudgeEndLabel = new GridBagConstraints();
		gbc_lblNudgeEndLabel.insets = new Insets(0, 0, 0, 5);
		gbc_lblNudgeEndLabel.gridx = 3;
		gbc_lblNudgeEndLabel.gridy = 2;
		panelScreenshotControls.add(lblNudgeEndLabel, gbc_lblNudgeEndLabel);
		GridBagConstraints gbc_buttonNudgeLeftEnd = new GridBagConstraints();
		gbc_buttonNudgeLeftEnd.anchor = GridBagConstraints.NORTH;
		gbc_buttonNudgeLeftEnd.insets = new Insets(0, 0, 0, 5);
		gbc_buttonNudgeLeftEnd.gridx = 4;
		gbc_buttonNudgeLeftEnd.gridy = 2;
		panelScreenshotControls.add(buttonNudgeLeftEnd, gbc_buttonNudgeLeftEnd);
		GridBagConstraints gbc_buttonNudgeRightEnd = new GridBagConstraints();
		gbc_buttonNudgeRightEnd.anchor = GridBagConstraints.NORTH;
		gbc_buttonNudgeRightEnd.gridx = 5;
		gbc_buttonNudgeRightEnd.gridy = 2;
		panelScreenshotControls.add(buttonNudgeRightEnd, gbc_buttonNudgeRightEnd);
		
		
		JPanel panelHelpText = new JPanel();
		panel.add(panelHelpText, "cell 0 4,growx,aligny center");
		
		JLabel lblHelpText = new JLabel("<html><body style='width:550px'>After setting an input/output file, change the start/end time to the part of the video you want to capture. Try to keep it short to stay under the file size limit, or else it will stop converting once it hits the limit.<br/><br/>TIP: If the generation takes a very long time, try restarting with the lowest quality settings and gradually work your way up from there.</body></html>");
		panelHelpText.add(lblHelpText);
		
		JPanel panelBottom = new JPanel();
		panel.add(panelBottom, "cell 0 5,growx,aligny center");
		
		btnMakeWebm = new JButton("Make WebM");
		btnMakeWebm.setPreferredSize(new Dimension(150, 30));
		
		chckbxPlayWhenDone = new JCheckBox("Play when done");
		
		JButton btnOpenContainingFolder = new JButton("Open containing folder");
		btnOpenContainingFolder.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				OpenContainingFolderButton_Clicked();
			}
		});
		panelBottom.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		lblProgressBar = new JLabel();
		
		panelBottom.add(lblProgressBar);
		
		progressBar = new JProgressBar();
		progressBar.setVisible(false);
		progressBar.setStringPainted(true);
		panelBottom.add(progressBar);
		panelBottom.add(btnMakeWebm);
		
		btnCancel = new JButton("Cancel");
		btnCancel.setVisible(false);
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CancelButton_Clicked();
			}
		});
		panelBottom.add(btnCancel);
		panelBottom.add(chckbxPlayWhenDone);
		panelBottom.add(btnOpenContainingFolder);
		btnMakeWebm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				MakeWebM_Clicked();
			}
		});
		

		
		LoadPreferences();
	}

	protected void DeleteScreenshotFiles()
	{
		File file;
		file = new File("preview_endFrame.jpg");
		file.delete();
		file = new File("preview_startFrame.jpg");
		file.delete();
	}

	protected void CancelButton_Clicked()
	{
		if(p != null)
			p.destroy();
	}

	private void LoadPreferences()
	{
		chckbxOverwrite.setSelected(prefs.getBoolean("Overwrite", true));
		chckbxPlayWhenDone.setSelected(prefs.getBoolean("PlayWhenDone", true));
		chckbxNoAudio.setSelected(prefs.getBoolean("NoAudio", true));
		textFieldMaxFileSize.setText(prefs.get("MaxFileSize", "3000000"));
		spinnerThreads.setValue(prefs.getInt("Threads", 4));
		spinnerCpuUsed.setValue(prefs.getInt("CpuUsed", 5));
		spinnerFrameRate.setValue(prefs.getInt("FrameRate", 24));
		spinnerBitrate.setValue(prefs.getInt("Bitrate", 64));
		spinnerScale.setValue(prefs.getInt("Scale", 100));
	}
	
	protected void SavePreferences()
	{
		prefs.putBoolean("Overwrite", chckbxOverwrite.isSelected());
		prefs.putBoolean("PlayWhenDone", chckbxPlayWhenDone.isSelected());
		prefs.putBoolean("NoAudio", chckbxNoAudio.isSelected());
		prefs.put("MaxFileSize", textFieldMaxFileSize.getText());
		prefs.putInt("Threads", (int)spinnerThreads.getValue());
		prefs.putInt("CpuUsed", (int)spinnerCpuUsed.getValue());
		prefs.putInt("FrameRate", (int)spinnerFrameRate.getValue());
		prefs.putInt("Bitrate", (int)spinnerBitrate.getValue());
		prefs.putInt("Scale", (int)spinnerScale.getValue());
	}

	protected void FileMenu_File_Close_Clicked()
	{
		System.exit(0);
	}

	protected void FileMenu_Help_HowDoIUseThis_Clicked()
	{
		String message = "<html><body style=\"width:350px;\">";
		message += "If you don't trust the ffmpeg.exe file packaged with this program then you can download it yourself from the official website ffmpeg.org/download.html. This program will not work unless ffmpeg.exe is next to this .jar. The source code for this program can be found in the /src folder of this .jar.";
		message += "<ol>";
		message += "<li>Select a video file using the Input Video... button.</li>";
		message += "<li>Set the time range using the slider bar by dragging the blue and red circles which represent the start and end time.<br/>";
		message += "You can also use the arrow buttons to nudge the start/end time by 1s or by 0.02s.</li>";
		message += "<li>Click Make WebM. The genereated file will be placed next to this .jar file when it's done.</li>";
		message += "</ol>";
		message += "<div style=\"text-align:right\">Author: Zyin</div>";
		message += "<div style=\"text-align:right\">Version " + VERSION + " - " + RELEASE_DATE + "</div>";
		message += "</body></html>";
		JOptionPane.showMessageDialog(menuBar, message, "How do I use this?", JOptionPane.INFORMATION_MESSAGE);
	}

	private void MakeWebM_Clicked()
	{
		if(GENERATING_WEBM)
		{
			return;
		}
		
		if(!InputAndOutputFieldsAreValid())
		{
			ShowFileChooser();
			return;
		}
		
		Thread thread = new Thread(){
			public void run(){
				GenerateWebM();
				
				if(chckbxPlayWhenDone.isSelected())
				{
					File file = new File (outputFileTextField.getText());
					Desktop desktop = Desktop.getDesktop();
					try {
						desktop.open(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.start();
	}
	
	private void GenerateWebM()
	{
		GENERATING_WEBM = true;
		
		rangeSlider.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		long timeStart = System.currentTimeMillis();
		
		lblProgressBar.setText("");
		progressBar.setValue(0);
		btnMakeWebm.setVisible(false);
		progressBar.setVisible(true);
		btnCancel.setVisible(true);

		String inputFileName = inputFileTextField.getText();
		String outputFileName = outputFileTextField.getText();
		
		if(inputFileName.isEmpty() || outputFileName.isEmpty())
		{
			return;
		}
		
		if(!outputFileName.toLowerCase().endsWith(".webm"))
		{
			outputFileName = outputFileName + ".webm";
			outputFileTextField.setText(outputFileName);
		}
		
		
		double startTime = rangeSliderStartTime + nudgeStartTime;
		double endTime = (rangeSliderEndTime + nudgeEndTime) - startTime;
		
		String startTimeString = SecondsToClockFormat(rangeSliderStartTime) + String.format("%.2f", nudgeStartTime).substring(1);
		String endTimeString = SecondsToClockFormat((int)endTime) + String.format("%.2f", endTime - ((int)endTime)).substring(1);
		
		String noAudio = chckbxNoAudio.isSelected() ? "-an" : "";
		String overwrite = chckbxOverwrite.isSelected() ? "-y" : "";
		String scale = "scale=iw*" + ((int)spinnerScale.getValue())/100 + ":-1";
		String maxFileSize = (textFieldMaxFileSize.getText().isEmpty() ? "999999999" : Integer.parseInt(textFieldMaxFileSize.getText())).toString();
		String threads = spinnerThreads.getValue().toString();
		String cpuUsed = spinnerCpuUsed.getValue().toString();
		String framerate = spinnerFrameRate.getValue().toString();
		String bitrate = spinnerBitrate.getValue() + "k";
		
		String[] command = {"ffmpeg",
			"-i", "\"" + inputFileName + "\"",	//input file path
			"-ss", startTimeString,	//start time
			"-t", endTimeString,	//duration
			noAudio,	//no audio
			overwrite,	//overwrite
			"-vf", scale,	//scaling
			"-fs", maxFileSize,	//max file size in Bytes
			"-threads", threads,	//cpu threads
			"-cpu-used", cpuUsed,	//encoding quality
			"-r", framerate,	//fps
			"-b:v", bitrate,	//variable bitrate
			//"-b", bitrate,	//static bitrate
			//"-crf", "0",	//constant bitrate mode, 0?4?-63 (0?4? = best quality)
			"-c:v", "libvpx",	//webm required video codec
			//"-qscale", "10",
			//"-qmin", "10",
			"-qmax", spinnerQMax.getValue().toString(),	//max compression (0-51) higher = more compressed (anything under 4 doesn't seem to work) (40 seems good)
			"\"" + outputFileName + "\""};	//output file name (with .webm extension)
		
		ExecuteCommand(command);
		
		Toolkit.getDefaultToolkit().beep();

		long timeEnd = System.currentTimeMillis();
		long timeElapsed = timeEnd - timeStart;
		String timeElapsedString = String.format("%.1f", (float)timeElapsed/1000);
		
		File file = new File(outputFileName);
		DecimalFormat formatter = new DecimalFormat("###,###");
		String fileSizeString = formatter.format(file.length()/1024) + " kB";
		
		btnMakeWebm.setVisible(true);
		progressBar.setVisible(false);
		btnCancel.setVisible(false);
		lblProgressBar.setText("Elapsed time: " + timeElapsedString + "s | Size: " + fileSizeString);
		
		rangeSlider.setCursor(Cursor.getDefaultCursor());
		
		GENERATING_WEBM = false;
	}
	
	private boolean InputAndOutputFieldsAreValid()
	{
		if(inputFileTextField.getText() == null || inputFileTextField.getText().isEmpty())
		{
			return false;
		}
		if(outputFileTextField.getText() == null || outputFileTextField.getText().isEmpty())
		{
			return false;
		}
		return true;
	}
	
	private String ExecuteCommand(String[] command) {
		String output = null;

        try
        {
        	ProcessBuilder builder = new ProcessBuilder(command);
        	builder.redirectErrorStream(true);
        	p = builder.start();
        	
        	String line = null;
        	BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        	
        	while ((line = reader.readLine()) != null)
        	{
        	    System.out.println ("Stdout: " + line);
        	    output += line;
        	    
        	    if(GENERATING_WEBM)
        	    	UpdateProgressBar(line);
        	}
        	
        }
        catch (IOException e)
        {
            System.out.println("Exception happened - here's what I know:\n");
            e.printStackTrace();
        }
        
        return output;
	}

	private void UpdateProgressBar(String line)
	{
		Pattern pattern = Pattern.compile("(?<=frame=)[^fps]*");	//matches "Frame=   318"
		Matcher matcher = pattern.matcher(line);
		if (matcher.find())
		{
			String frameString = matcher.group().trim();
			int currentFrame = Integer.parseInt(frameString);
			int totalFrames = (rangeSliderEndTime - rangeSliderStartTime) * (int)spinnerFrameRate.getValue();
			double percentDouble = (currentFrame == 0 ? 0 : ((double)currentFrame / totalFrames));
			int percent = (int)(percentDouble*100);
			
			progressBar.setValue(percent);
			
	    	UpdateEstimatedFileSize(line, percent);
		}
	}
	private void UpdateEstimatedFileSize(String line, int percent)
	{
		Pattern pattern = Pattern.compile("(?<=size=)[^kB]*");	//matches "size=       409"
		Matcher matcher = pattern.matcher(line);
		if (matcher.find())
		{
			String sizeString = matcher.group().trim();
			int currentSize = Integer.parseInt(sizeString);
			int estimatedFileSize = (int) (currentSize / (percent/100.0));
			
			DecimalFormat formatter = new DecimalFormat("###,###");
			String estimatedFileSizeString = formatter.format(estimatedFileSize);
			
			lblProgressBar.setText("Estimated video size: " + estimatedFileSizeString + " kB");
		}
	}
	
	private void InputFile_Clicked()
	{
		ShowFileChooser();
	}
	
	private void FileMenu_Open_Clicked()
	{
		ShowFileChooser();
	}
	
	private void ShowFileChooser()
	{
		if(GENERATING_WEBM)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		
		int returnVal = fc.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            File file = fc.getSelectedFile();

		    prefs.put("LastUsedFolder", file.getParent());
		    
		    String fileNameWithExtension = file.getName();
            int pos = fileNameWithExtension.lastIndexOf(".");
            if (pos > 0)
            	fileNameWithExtension = fileNameWithExtension.substring(0, pos);
            
            inputFileTextField.setText(file.getAbsolutePath());
            outputFileTextField.setText(fileNameWithExtension + ".webm");
            
            ClearScreenshots();
            UpdateRangeSliderTimeRange();
            
        }
	}
	

	private void ClearScreenshots()
	{
        ImageIcon icon = new ImageIcon();
        lblPreviewStart.setIcon(icon);
        lblPreviewEnd.setIcon(icon);
	}

	private void UpdateRangeSliderTimeRange()
	{
        String[] command = {"ffmpeg",
            	"-i", "\"" + inputFileTextField.getText() + "\"",	//input file path
            	"2>&1 | grep \"Duration\""};
        
        String commandOutput = ExecuteCommand(command);

    	if(commandOutput == null)
    	{
    		String message = "<html><body>";
    		message += "Could not grab output from ffmpeg.";
    		message += "</body></html>";
    		JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);

    		inputFileTextField.setText("");
    		outputFileTextField.setText("");
    		return;
    	}
        
        Pattern pattern = Pattern.compile("(?<=Duration: )[^,]*");	//matches "Duration: 00:41:18.57" -> "00:41:18.57"
        Matcher matcher = pattern.matcher(commandOutput);
        if (matcher.find())
        {
        	String duration = matcher.group();
        	
        	if(duration.equals("N/A") || duration.equals("00:00:00.00") || duration.equals("00:00:00.01"))
        	{
        		String message = "<html><body>";
        		message += "This video could not load because there was no information about its duration (Duration: " + duration + ").";
        		message += "</body></html>";
        		JOptionPane.showMessageDialog(null, message, "Unsupported video", JOptionPane.ERROR_MESSAGE);

        		inputFileTextField.setText("");
        		outputFileTextField.setText("");
        		return;
        	}
        	
        	String[] durationArray = duration.split(":");
        	String hours = durationArray[0];
        	String minutes = durationArray[1];
        	String secondsAndDecaSeconds = durationArray[2];
        	String[] decaSecondsArray = secondsAndDecaSeconds.split("\\.");
        	String seconds = decaSecondsArray[0];
        	//String decaSeconds = decaSecondsArray[1];
        	
        	int sec = Integer.parseInt(hours) * 60 * 60 + 
        			Integer.parseInt(minutes) * 60 + 
        			Integer.parseInt(seconds);

        	DISABLE_SLIDER_CHANGE_EVENTS = true;
            //rangeSlider.setMinimum(0);
            rangeSlider.setMaximum(sec);
            rangeSlider.setUpperValue((int)2*sec/3);
            rangeSlider.setValue((int)(sec/3.0));
            
            RangeSliderUpper_Changed(rangeSlider);
            RangeSliderLower_Changed(rangeSlider);
        	DISABLE_SLIDER_CHANGE_EVENTS = false;
        }
        else
        {
        	String message = "<html><body>";
    		message += "This file type is unsupported.";
    		message += "</body></html>";
    		JOptionPane.showMessageDialog(null, message, "Unsupported file type", JOptionPane.ERROR_MESSAGE);

    		inputFileTextField.setText("");
    		outputFileTextField.setText("");
    		return;
        }
	}

	private void NudgeButtons_Click(boolean nudgeTheStartTime, float amount)
	{
		if(GENERATING_WEBM)
		{
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		
		if(nudgeTheStartTime)
		{
			if(nudgeStartTime + amount*2 >= 1 || nudgeStartTime + amount < 0)
			{
				
			}
			else
			{
				nudgeStartTime += amount;
				GenerateStartScreenshotPreview();
			}
		}
		else
		{
			if(nudgeEndTime + amount*2 >= 1 || nudgeEndTime + amount < 0)
			{
				
			}
			else
			{
				nudgeEndTime += amount;
				GenerateEndScreenshotPreview();
			}
		}
		
		
		UpdateNudgeTimeLabels();
	}

	private void LargeNudgeButtons_Clicked(boolean nudgeTheStartTime, int amount)
	{
		if(nudgeTheStartTime)
		{
			int startValue = rangeSlider.getValue();
			if(startValue + amount > rangeSlider.getUpperValue() || startValue + amount < 0)
			{
				
			}
			else
			{
				rangeSlider.setValue(startValue + amount);
				GenerateStartScreenshotPreview();
				RangeSliderLower_Changed(rangeSlider);
			}
		}
		else
		{
			int endValue = rangeSlider.getUpperValue();
			if(endValue + amount > rangeSlider.getMaximum() || endValue + amount < rangeSlider.getValue())
			{
				
			}
			else
			{
				rangeSlider.setUpperValue(endValue + amount);
				GenerateEndScreenshotPreview();
				RangeSliderUpper_Changed(rangeSlider);
			}
		}
	}

	private void UpdateNudgeTimeLabels()
	{
		UpdateNudgeStartTimeLabel();
		UpdateNudgeEndTimeLabel();
	}
	private void UpdateNudgeStartTimeLabel()
	{
		if(nudgeStartTime >= 0)
			lblNudgeStartLabel.setText("+" + String.format("%.2f", nudgeStartTime) + "s");
		else
			lblNudgeStartLabel.setText(String.format("%.2f", nudgeStartTime) + "s");
	}
	private void UpdateNudgeEndTimeLabel()
	{
		if(nudgeEndTime >= 0)
			lblNudgeEndLabel.setText("+" + String.format("%.2f", nudgeEndTime) + "s");
		else
			lblNudgeEndLabel.setText(String.format("%.2f", nudgeEndTime) + "s");
	}

	private void RangeSliderUpper_Changing(RangeSlider slider)
	{
		lblEndLabel.setText(SecondsToClockFormat(slider.getUpperValue()));
	}
	private void RangeSliderLower_Changing(RangeSlider slider)
	{
		lblStartLabel.setText(SecondsToClockFormat(slider.getValue()));
	}

	private void RangeSliderUpper_Changed(RangeSlider slider)
	{
		if(!InputAndOutputFieldsAreValid())
		{
			return;
		}
		
		UpdateRangeSliderEndTime();

		GenerateEndScreenshotPreview();

		nudgeEndTime = 0;
		UpdateNudgeEndTimeLabel();
	}
	private void RangeSliderLower_Changed(RangeSlider slider)
	{
		if(!InputAndOutputFieldsAreValid())
		{
			return;
		}
		
		UpdateRangeSliderStartTime();

		GenerateStartScreenshotPreview();

		nudgeStartTime = 0;
		UpdateNudgeTimeLabels();
	}


	private void OpenContainingFolderButton_Clicked()
	{
		String path = WebmConverterGui.class.getProtectionDomain().getCodeSource().getLocation().toString();
		path = path.substring(0, path.lastIndexOf('/'));
		
		try {
			Runtime.getRuntime().exec("explorer \""+path+"\"");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void UpdateRangeSliderStartTime()
	{
		String startString = SecondsToClockFormat(rangeSlider.getValue());

		rangeSliderStartTime = rangeSlider.getValue();
		
	    lblStartLabel.setText(startString);
	}
	private void UpdateRangeSliderEndTime()
	{
		String endString = SecondsToClockFormat(rangeSlider.getUpperValue());

		rangeSliderEndTime = rangeSlider.getUpperValue();
		
	    lblEndLabel.setText(endString);
	}

	
	private void GenerateStartScreenshotPreview()
	{
		if(inputFileTextField.getText().isEmpty())
		{
			return;
		}
		
		rangeSlider.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		String startTime = SecondsToClockFormat(rangeSliderStartTime) + String.format("%.2f", nudgeStartTime).substring(1);
		
		String[] command = {"ffmpeg",
			"-ss", startTime,	//start time
			"-i",  "\"" + inputFileTextField.getText() + "\"",	//input file path
			"-vf", "scale=-1:200",
			"-y",	//overwrite
			"-f", "image2",	//force format
			"-vcodec", "mjpeg",
			"-vframes", "1",
			"preview_startFrame.jpg"};
		
		ExecuteCommand(command);

		UpdateStartImage("preview_startFrame.jpg");

		rangeSlider.setCursor(Cursor.getDefaultCursor());
	}

	private void GenerateEndScreenshotPreview()
	{
		if(inputFileTextField.getText().isEmpty())
		{
			return;
		}
		
		rangeSlider.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		String endTime = SecondsToClockFormat(rangeSliderEndTime) + String.format("%.2f", nudgeEndTime).substring(1);
		
		String[] command = {"ffmpeg",
			"-ss", endTime,	//start time
			"-i", "\"" + inputFileTextField.getText() + "\"",	//input file path
			"-vf", "scale=-1:200",	//scale width:height
			"-y",	//overwrite
			"-f", "image2",	//force format
			"-vcodec", "mjpeg",
			"-vframes", "1",
			"preview_endFrame.jpg"};
		
		ExecuteCommand(command);
	
		UpdateEndImage("preview_endFrame.jpg");
		
		rangeSlider.setCursor(Cursor.getDefaultCursor());
	}

	private String SecondsToClockFormat(int s)
	{
		int hours = (int) Math.floor(s / 3600);
		int minutes = (int) Math.floor((s / 60) % 60);
		int seconds = s % 60;
		
		return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
	}
	
	/*private int ClockFormatToSeconds(String time)
	{
		String[] timeArray = time.split(":");
		int hours = Integer.parseInt(timeArray[0]);
		int minutes = Integer.parseInt(timeArray[1]);
		int seconds = Integer.parseInt(timeArray[2]);
		
		return hours * 3600 + minutes * 60 + seconds;
	}*/
	
	

    public void UpdateStartImage(String fileName)
    {
    	if(fileName == null || fileName.isEmpty())
    	{
    		return;
    	}
    	
        BufferedImage img = null;
		try
		{
			img = ImageIO.read(new File(fileName));
	        ImageIcon icon = new ImageIcon(img);
	        lblPreviewStart.setIcon(icon);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
    }
    
    public void UpdateEndImage(String fileName)
    {
    	if(fileName == null || fileName.isEmpty())
    	{
    		return;
    	}
    	
        BufferedImage img = null;
		try 
		{
			img = ImageIO.read(new File(fileName));
	        ImageIcon icon = new ImageIcon(img);
	        lblPreviewEnd.setIcon(icon);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
    }
}

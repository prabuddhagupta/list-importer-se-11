package com.se.rdc.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author omar
 */
public class CaptchaPrompt extends javax.swing.JFrame {

  private javax.swing.JButton jButton;
  private javax.swing.JPanel jPanel;
  private javax.swing.JTextField jTextField;
  private String capImgUrl;
  private JLabel jLabel;
  public boolean isSubmitted = false;
  
  public CaptchaPrompt() {
    setLookNFeel();    
  }
  
  public CaptchaPrompt(String capurl) {
    this.capImgUrl = capurl;
    setLookNFeel();
  }

  private void setLookNFeel() {
    try {
      for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager
          .getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (ClassNotFoundException ex) {
      java.util.logging.Logger.getLogger(CaptchaPrompt.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      java.util.logging.Logger.getLogger(CaptchaPrompt.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      java.util.logging.Logger.getLogger(CaptchaPrompt.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
      java.util.logging.Logger.getLogger(CaptchaPrompt.class.getName()).log(
          java.util.logging.Level.SEVERE, null, ex);
    }
  }

  public void setCaptcha(String url) throws IOException {
    BufferedImage image = ImageIO.read(new URL(url));
    
    int h = image.getHeight();
    int w = image.getWidth();
    int jh = jPanel.getHeight();
    int jw = jPanel.getWidth();
    
    int top = 0,left = 0;
    if(h<jh){
      top = (jh-h)/2;
    }
    if(w<jw){
      left = (jw-w)/2;
    }
    
    jPanel.getGraphics().drawImage(image, left, top, null);
  }

  @SuppressWarnings("unchecked")
  private void initComponents() {

            jTextField = new javax.swing.JTextField();
        jButton = new javax.swing.JButton();
        jPanel = new javax.swing.JPanel();
        jLabel = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Enter captcha text");
        setAlwaysOnTop(true);
        setLocationByPlatform(true);
        setMinimumSize(new java.awt.Dimension(480, 210));
        setResizable(false);
        setSize(new java.awt.Dimension(0, 0));

        jTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldActionPerformed(evt);
            }
        });

        jButton.setText("Submit");
        jButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonActionPerformed(evt);
            }
        });

        jPanel.setBackground(java.awt.Color.lightGray);

        javax.swing.GroupLayout jPanelLayout = new javax.swing.GroupLayout(jPanel);
        jPanel.setLayout(jPanelLayout);
        jPanelLayout.setHorizontalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanelLayout.setVerticalGroup(
            jPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 151, Short.MAX_VALUE)
        );

        jLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel.setText("Waiting for captcha submission ...");
        jLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel.setVerifyInputWhenFocusTarget(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel))
        );

        pack();
        setLocationRelativeTo(null);
  }

  private void jButtonActionPerformed(java.awt.event.ActionEvent evt) {
    validateTextInput();
  }

  private void jTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
    validateTextInput();
  }

  private void showPopUp(String msg){
    JOptionPane.showMessageDialog(this, msg);    
  }
    
  private void validateTextInput(){
    if(jTextField.getText().trim().length()<1){
      showPopUp("Please enter the text from the captcha image");
    }else{
      updateStatus("Varifying the captcha, please wait...");
      //dispose();
      isSubmitted = true;
    }
  }
  
  private void updateStatus(String text){
    jLabel.setText(text);
  }
  
  public String getCaptchaText(){
    return jTextField.getText();
  }
  
  public void successfulCaptcha(){
    showPopUp("Captcha matched sucessfully!!");
    dispose();
  }
  
  public void updateCapcha(String imgUrl) throws MalformedURLException, IOException{
    updateStatus("Wrong captcha! Please re-enter the captcha text.");
    setCaptcha(imgUrl);
    isSubmitted = false;
  }
  
  public void run() throws MalformedURLException, IOException {
    /* Create and display the form */
//    java.awt.EventQueue.invokeLater(new Runnable() {
//      public void run() {
//        new CaptchaPrompt().setVisible(true);
//      }
//    });
    
    initComponents();
    setVisible(true);
    if(capImgUrl!=null){
      setCaptcha(capImgUrl);
    }
  }
}

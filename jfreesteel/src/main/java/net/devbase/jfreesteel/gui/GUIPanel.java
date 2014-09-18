/*
 * jfreesteel: Serbian eID Viewer Library (GNU LGPLv3)
 * Copyright (C) 2011 Goran Rakic
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see
 * http://www.gnu.org/licenses/.
 */

package net.devbase.jfreesteel.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import net.devbase.jfreesteel.EidInfo;

/**
 * Use GUIPanel as JPanel in your Swing applications to display data from the eID card.
 *
 * See SerbianEidViewer application for the example how to use GUIPanel, Reader and EidCard to
 * create the complete eID card viewer. Please note that the SerbianEidViewer is released under GNU
 * Affero GPLv3 license, while this class and the rest of JFreesteel library is released under the
 * more permissive  GNU Lesser GPL license version 3.
 *
 * @author Goran Rakic (grakic@devbase.net)
 */
public class GUIPanel extends JPanel {

    private static final long serialVersionUID = 5830429844217109957L;

    private static final ResourceBundle bundle = ResourceBundle.getBundle(
        "net.devbase.jfreesteel.gui.jfreesteel-lib-gui"); //$NON-NLS-1$

    private JImagePanel photo;
    private Image throbber;

    protected JPanel toolbar;

    private JLabel nameFull;
    private JLabel personalNumber;
    private JLabel placeFull;
    private JLabel addressDate;
    private JLabel addressDateLabel;
    private JLabel dateOfBirth;
    private JLabel placeOfBirthFull;
    private JLabel docRegNo;
    private JLabel issuingDate;
    private JLabel expiryDate;
    private JLabel issuingAuthority;

    public void clearDetailsAndPhoto() {
        setDetails("", "", "", "", "", "", "", "", "", "");
        setPhoto(null);
    }

    public void setPhoto(Image image) {
        if(image == null) {
            photo.setImage(throbber);
        } else {
            photo.setImage(image);
        }
    }

    public void setDetails(EidInfo info) {
        nameFull.setText(info.getNameFull());
        personalNumber.setText(info.getPersonalNumber());
        dateOfBirth.setText(info.getDateOfBirth());
        placeOfBirthFull.setText("<html>"+info.getPlaceOfBirthFull().replace("\n", "<br/>"));

        placeFull.setText("<html>"+info.getPlaceFull(bundle.getString("EntranceLabelFormat"),
            bundle.getString("FloorLabelFormat"),
            bundle.getString("AppartmentLabelFormat")).replace("\n", "<br/>"));

        setAddressDate(info.getAddressDate());

        docRegNo.setText(info.getDocRegNo());
        issuingDate.setText(info.getIssuingDate());
        expiryDate.setText(info.getExpiryDate());
        issuingAuthority.setText(info.getIssuingAuthority());
    }

    public void setDetails(String nameFull, String personalNumber, String placeFull, String addressDate,
        String dateOfBirth, String placeOfBirthFull, String docRegNo, String issuingDate,
        String expiryDate, String issuingAuthority) {

        this.nameFull.setText(nameFull);
        this.personalNumber.setText(personalNumber);
        this.dateOfBirth.setText(dateOfBirth);
        this.placeOfBirthFull.setText("<html>"+placeOfBirthFull.replace("\n", "<br/>"));

        this.placeFull.setText("<html>"+placeFull.replace("\n", "<br/>"));
        setAddressDate(addressDate);

        this.docRegNo.setText(docRegNo);
        this.issuingDate.setText(issuingDate);
        this.expiryDate.setText(expiryDate);
        this.issuingAuthority.setText(issuingAuthority);
    }

    private void setAddressDate(String addressDate) {
        if (addressDate != null && addressDate.length() > 0) {
            this.addressDate.setText(addressDate);
            this.addressDate.setVisible(true);
            this.addressDateLabel.setVisible(true);
        } else {
            this.addressDate.setText("");
            this.addressDate.setVisible(false);
            this.addressDateLabel.setVisible(false);
        }
    }

    public GUIPanel() {
        setSize(new Dimension(730, 320));  // without layout manager
        setPreferredSize(new Dimension(730, 320));  // with layout manager
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        URL throbberURL = GUIPanel.class.getResource("/net/devbase/jfreesteel/gui/throbber.gif");
        throbber = Toolkit.getDefaultToolkit().createImage(throbberURL);

        photo = new JImagePanel(throbber);
        photo.setMaximumSize(new Dimension(240, 320));
        photo.setPreferredSize(new Dimension(240, 320));
        photo.setMinimumSize(new Dimension(240, 320));
        photo.setBackground(new Color(128, 128, 128));
        photo.setBorder(new EmptyBorder(12, 12, 12, 12));
        add(photo);

        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(0, 24, 0, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        add(panel);

        JPanel panel_1 = new JPanel();
        panel_1.setBorder(new EmptyBorder(12, 0, 24, 0));
        panel_1.setAlignmentX(0.0f);
        panel.add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

        nameFull = new JLabel("");
        nameFull.setHorizontalTextPosition(SwingConstants.LEADING);
        nameFull.setFont(nameFull.getFont().deriveFont(nameFull.getFont().getStyle() | Font.BOLD, nameFull.getFont().getSize() + 8f));
        panel_1.add(nameFull);
        
        JPanel panel_2 = new JPanel();
        panel_2.setAlignmentX(0.0f);
        panel.add(panel_2);
        GridBagLayout gbl_panel_2 = new GridBagLayout();
        gbl_panel_2.columnWidths = new int[]{110, 0};
        gbl_panel_2.rowHeights = new int[]{0, 0, 0, 0, 0};
        gbl_panel_2.columnWeights = new double[]{0.0, 1.0};
        gbl_panel_2.rowWeights = new double[]{1.0, 1.0, 1.0, 1.0, 3.0};
        panel_2.setLayout(gbl_panel_2);

        JLabel personalNumberLabel = new JLabel(bundle.getString("PersonalNumber")); //$NON-NLS-1$
        personalNumberLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_personalNumberLabel = new GridBagConstraints();
        gbc_personalNumberLabel.anchor = GridBagConstraints.WEST;
        // top, left, bottom, right
        gbc_personalNumberLabel.insets = new Insets(0, 0, 6, 12);
        gbc_personalNumberLabel.gridx = 0;
        gbc_personalNumberLabel.gridy = 0;
        panel_2.add(personalNumberLabel, gbc_personalNumberLabel);

        personalNumber = new JLabel("");
        GridBagConstraints gbc_personalNumber = new GridBagConstraints();
        gbc_personalNumber.anchor = GridBagConstraints.NORTH;
        gbc_personalNumber.fill = GridBagConstraints.HORIZONTAL;
        gbc_personalNumber.gridx = 1;
        gbc_personalNumber.gridy = 0;
        panel_2.add(personalNumber, gbc_personalNumber);

        JLabel placeFullLabel = new JLabel(bundle.getString("Place")); //$NON-NLS-1$
        placeFullLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_placeFullLabel = new GridBagConstraints();
        gbc_placeFullLabel.anchor = GridBagConstraints.NORTHWEST;
        gbc_placeFullLabel.insets = new Insets(0, 0, 6, 12);
        gbc_placeFullLabel.gridx = 0;
        gbc_placeFullLabel.gridy = 1;
        panel_2.add(placeFullLabel, gbc_placeFullLabel);

        placeFull = new JLabel("");
        GridBagConstraints gbc_placeFull = new GridBagConstraints();
        gbc_placeFull.anchor = GridBagConstraints.NORTH;
        gbc_placeFull.fill = GridBagConstraints.HORIZONTAL;
        gbc_placeFull.gridx = 1;
        gbc_placeFull.gridy = 1;
        panel_2.add(placeFull, gbc_placeFull);

        addressDateLabel = new JLabel(bundle.getString("AddressDate")); //$NON-NLS-1$
        addressDateLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_addressDateLabel = new GridBagConstraints();
        gbc_addressDateLabel.anchor = GridBagConstraints.NORTHWEST;
        gbc_addressDateLabel.insets = new Insets(0, 0, 6, 12);
        gbc_addressDateLabel.gridx = 0;
        gbc_addressDateLabel.gridy = 2;
        panel_2.add(addressDateLabel, gbc_addressDateLabel);

        addressDate = new JLabel("");
        GridBagConstraints gbc_addressDate = new GridBagConstraints();
        gbc_addressDate.anchor = GridBagConstraints.NORTH;
        gbc_addressDate.fill = GridBagConstraints.HORIZONTAL;
        gbc_addressDate.gridx = 1;
        gbc_addressDate.gridy = 3;
        panel_2.add(addressDate, gbc_addressDate);

        JLabel dateOfBirthLabel = new JLabel(bundle.getString("DateOfBirth")); //$NON-NLS-1$
        dateOfBirthLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_dateOfBirthLabel = new GridBagConstraints();
        gbc_dateOfBirthLabel.anchor = GridBagConstraints.NORTHWEST;
        gbc_dateOfBirthLabel.insets = new Insets(0, 0, 6, 12);
        gbc_dateOfBirthLabel.gridx = 0;
        gbc_dateOfBirthLabel.gridy = 3;
        panel_2.add(dateOfBirthLabel, gbc_dateOfBirthLabel);

        dateOfBirth = new JLabel("");
        GridBagConstraints gbc_dateOfBirth = new GridBagConstraints();
        gbc_dateOfBirth.anchor = GridBagConstraints.NORTH;
        gbc_dateOfBirth.fill = GridBagConstraints.HORIZONTAL;
        gbc_dateOfBirth.gridx = 1;
        gbc_dateOfBirth.gridy = 3;
        panel_2.add(dateOfBirth, gbc_dateOfBirth);

        JLabel placeOfBirthFullLabel = new JLabel(bundle.getString("PlaceOfBirth")); //$NON-NLS-1$
        placeOfBirthFullLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_placeOfBirthFullLabel = new GridBagConstraints();
        gbc_placeOfBirthFullLabel.anchor = GridBagConstraints.NORTHWEST;
        gbc_placeOfBirthFullLabel.insets = new Insets(0, 0, 6, 12);
        gbc_placeOfBirthFullLabel.gridx = 0;
        gbc_placeOfBirthFullLabel.gridy = 4;
        panel_2.add(placeOfBirthFullLabel, gbc_placeOfBirthFullLabel);

        placeOfBirthFull = new JLabel("");
        GridBagConstraints gbc_placeOfBirthFull = new GridBagConstraints();
        gbc_placeOfBirthFull.anchor = GridBagConstraints.NORTH;
        gbc_placeOfBirthFull.fill = GridBagConstraints.HORIZONTAL;
        gbc_placeOfBirthFull.gridx = 1;
        gbc_placeOfBirthFull.gridy = 4;
        panel_2.add(placeOfBirthFull, gbc_placeOfBirthFull);

        toolbar = new JPanel();
        toolbar.setAlignmentX(0.0f);
        panel.add(toolbar);

        JPanel panel_4 = new JPanel();
        panel_4.setAlignmentX(0.0f);
        panel.add(panel_4);
        GridBagLayout gbl_panel_4 = new GridBagLayout();
        gbl_panel_4.columnWidths = new int[]{110, 0, 0, 0};
        gbl_panel_4.rowHeights = new int[]{0, 0, 0};
        gbl_panel_4.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0};
        gbl_panel_4.rowWeights = new double[]{1.0, 0.0, 0.0};
        panel_4.setLayout(gbl_panel_4);

        JLabel docRegNoLabel = new JLabel(bundle.getString("DocRegNo")); //$NON-NLS-1$
        docRegNoLabel.setAlignmentX(1.0f);
        docRegNoLabel.setHorizontalAlignment(SwingConstants.LEFT);
        docRegNoLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        docRegNoLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_docRegNoLabel = new GridBagConstraints();
        gbc_docRegNoLabel.anchor = GridBagConstraints.WEST;
        gbc_docRegNoLabel.gridx = 0;
        gbc_docRegNoLabel.gridy = 1;
        gbc_docRegNoLabel.insets = new Insets(0, 0, 0, 12);
        panel_4.add(docRegNoLabel, gbc_docRegNoLabel);

        JLabel issuingDateLabel = new JLabel(bundle.getString("IssuingDate")); //$NON-NLS-1$
        issuingDateLabel.setAlignmentX(1.0f);
        issuingDateLabel.setHorizontalAlignment(SwingConstants.LEFT);
        issuingDateLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        issuingDateLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_issuingDateLabel = new GridBagConstraints();
        gbc_issuingDateLabel.anchor = GridBagConstraints.WEST;
        gbc_issuingDateLabel.gridx = 1;
        gbc_issuingDateLabel.gridy = 1;
        panel_4.add(issuingDateLabel, gbc_issuingDateLabel);

        JLabel expiryDateLabel = new JLabel(bundle.getString("ExpiryDate")); //$NON-NLS-1$
        expiryDateLabel.setAlignmentX(1.0f);
        expiryDateLabel.setHorizontalAlignment(SwingConstants.LEFT);
        expiryDateLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        expiryDateLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_expiryDateLabel = new GridBagConstraints();
        gbc_expiryDateLabel.anchor = GridBagConstraints.WEST;
        gbc_expiryDateLabel.gridx = 2;
        gbc_expiryDateLabel.gridy = 1;
        panel_4.add(expiryDateLabel, gbc_expiryDateLabel);

        JLabel issuingAuthorityLabel = new JLabel(bundle.getString("IssuingAuthority")); //$NON-NLS-1$
        issuingAuthorityLabel.setAlignmentX(1.0f);
        issuingAuthorityLabel.setHorizontalAlignment(SwingConstants.LEFT);
        issuingAuthorityLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        issuingAuthorityLabel.setForeground(Color.GRAY);
        GridBagConstraints gbc_issuingAuthorityLabelLabel = new GridBagConstraints();
        gbc_issuingAuthorityLabelLabel.anchor = GridBagConstraints.WEST;
        gbc_issuingAuthorityLabelLabel.insets = new Insets(0, 0, 0, 6);
        gbc_issuingAuthorityLabelLabel.gridx = 3;
        gbc_issuingAuthorityLabelLabel.gridy = 1;
        panel_4.add(issuingAuthorityLabel, gbc_issuingAuthorityLabelLabel);

        docRegNo = new JLabel("");
        docRegNo.setAlignmentX(1.0f);
        docRegNo.setHorizontalAlignment(SwingConstants.LEFT);
        docRegNo.setHorizontalTextPosition(SwingConstants.LEFT);
        GridBagConstraints gbc_docRegNo = new GridBagConstraints();
        gbc_docRegNo.anchor = GridBagConstraints.WEST;
        gbc_docRegNo.gridx = 0;
        gbc_docRegNo.gridy = 2;
        panel_4.add(docRegNo, gbc_docRegNo);

        issuingDate = new JLabel("");
        issuingDate.setAlignmentX(1.0f);
        issuingDate.setHorizontalAlignment(SwingConstants.LEFT);
        issuingDate.setHorizontalTextPosition(SwingConstants.LEFT);
        GridBagConstraints gbc_issuingDate = new GridBagConstraints();
        gbc_issuingDate.anchor = GridBagConstraints.WEST;
        // top, left, right, bottom
        gbc_issuingDate.insets = new Insets(0, 0, 0, 24);
        gbc_issuingDate.gridx = 1;
        gbc_issuingDate.gridy = 2;
        panel_4.add(issuingDate, gbc_issuingDate);

        expiryDate = new JLabel("");
        expiryDate.setAlignmentX(1.0f);
        expiryDate.setHorizontalAlignment(SwingConstants.LEFT);
        expiryDate.setHorizontalTextPosition(SwingConstants.LEFT);
        GridBagConstraints gbc_expiryDate = new GridBagConstraints();
        gbc_expiryDate.anchor = GridBagConstraints.WEST;
        gbc_expiryDate.insets = new Insets(0, 0, 0, 24);
        gbc_expiryDate.gridx = 2;
        gbc_expiryDate.gridy = 2;
        panel_4.add(expiryDate, gbc_expiryDate);

        issuingAuthority = new JLabel("");
        issuingAuthority.setAlignmentX(1.0f);
        issuingAuthority.setHorizontalAlignment(SwingConstants.LEFT);
        issuingAuthority.setHorizontalTextPosition(SwingConstants.LEFT);
        GridBagConstraints gbc_issuingAuthority = new GridBagConstraints();
        gbc_issuingAuthority.anchor = GridBagConstraints.WEST;
        gbc_issuingAuthority.gridx = 3;
        gbc_issuingAuthority.gridy = 2;
        panel_4.add(issuingAuthority, gbc_issuingAuthority);
    }
}

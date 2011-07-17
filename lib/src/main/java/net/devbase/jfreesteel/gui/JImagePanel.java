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

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

/**
 * Simple JPanel with BufferedImage
 * 
 * @author Goran Rakic (grakic@devbase.net)
 */
public class JImagePanel extends JPanel {

    private static final long serialVersionUID = 2272776565547958916L;
    private Image image = null;

    public JImagePanel() {

    }
    
    public JImagePanel(final Image image) {
        this.image = image;
    }
    
    public void setImage(final Image image) {
        this.image = image;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(image != null) {
            g.drawImage(image, (this.getWidth()-image.getWidth(null))/2, (this.getHeight()-image.getHeight(null))/2, this);
        }
    }
}

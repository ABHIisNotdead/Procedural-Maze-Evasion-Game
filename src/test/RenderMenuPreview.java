package test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import ui.MainFrame;

public class RenderMenuPreview {
    public static void main(String[] args) throws Exception {
        MainFrame mf = new MainFrame();
        mf.setSize(700, 520);
        mf.doLayout();
        BufferedImage img = new BufferedImage(700, 520, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        mf.paint(g);
        g.dispose();
        File out = new File("C:/Users/Admin/.gemini/antigravity/brain/9235a269-dbab-44b8-a2f5-1b4679b775e8/menu_preview.png");
        ImageIO.write(img, "png", out);
        System.out.println("Menu preview saved.");
        System.exit(0);
    }
}

package bm.gfx;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Zweck dieser Klasse ist, alle verwendeten Texturen einzulesen und diese als
 * statische Objekte bereitzustellen, damit diese zB in BombermanLevel
 * gezeichnet werden können. Sie dient im Grunde als Wrapper fuer das von Java2D
 * bereitgestellte Objekt BufferedImage; zusaetzlich werden Methoden zum
 * effizienten (aber speicherlastigerem) Skalieren und Spiegeln sowie zum
 * ineffezienten Ersetzen der Farben bereitgestellt.
 * 
 * @author tobi
 * 
 */
public class Texture {
    /*
     * the following four colors are to (or can) be replaced for several
     * textures
     */
    private static final int HOUSECOLOR1 = 0xffcccccc; // grey
    private static final int HOUSECOLOR1_SHADOW = 0xffaaaaaa; // dark grey
    private static final int HOUSECOLOR2 = 0xffcccc00; // yellow
    private static final int HOUSECOLOR2_SHADOW = 0xffaaaa00; // dark yellow
    public static final int[] HOUSECOLORS = { HOUSECOLOR1, HOUSECOLOR1_SHADOW,
            HOUSECOLOR2, HOUSECOLOR2_SHADOW };

    // aktuell der Pfad zur Datei, welche die Texturen beinhaltet
    private static final String IMGURL = "/texture.png";

    public static final int[] TEXTCOLORS = new int[] { 0xff000000, 0xffffffff };

    /*
     * Statische Methode zum Einlesen eines Bildes, welches durch IMGURL gegeben
     * ist. Das Bild wird als BufferedImage zurück gegeben
     */
    private static BufferedImage getBufferedImage() {
        try {
            return ImageIO.read(new Texture().getClass().getResource(IMGURL));
        } catch (IOException e) {
            System.err.println("Fehler beim Laden der Texturen.");
            e.printStackTrace();
            return null;
        }
    }

    // benutzt obige Methode, um die Texturen einzulesen
    private static final BufferedImage TEXTURE = getBufferedImage();

    private BufferedImage texture; // Original-Bild zum verlustfreien Skalieren
    private BufferedImage scaledTexture; // tatsächlich gemaltes Bild

    private int width, height;

    /**
     * @deprecated
     * 
     * Dummy-Konstruktor, welcher kein brauchbares Objekt erzeugt. Wird nur
     * genutzt, um den relativen Pfad zusammen mit der getClass() Methode nutzen
     * zu koennen.
     */
    public Texture() {}// dummy-constructor für obige Methode.. das muss besser
                       // gehn!

    /**
     * Erzeugt ein neues Texturen-Objekt aus einem Teilbild von img/texture.png.
     * 
     * @param sx X-Koordinate des linken oberen Pixels des Teilbilds
     * @param sy Y-Koordinate des linken oberen Pixels des Teilbilds
     * @param width Breite des Teilbilds
     * @param height Hoehe des Teilbilds
     */
    public Texture(int sx, int sy, int width, int height) {
        scaledTexture = texture = TEXTURE.getSubimage(sx, sy, width, height);
        this.width = width;
        this.height = height;
    }

    /**
     * Erzeugt ein neues Texturen-Objekt anhand eines kompletten Bildes.
     * 
     * @param texture Das Bild, um welches das Texture-Objekt gewrappt wird.
     */
    public Texture(BufferedImage texture) {
        this.texture = scaledTexture = texture;
        width = texture.getWidth();
        height = texture.getHeight();
    }

    /**
     * Zeichnet die Textur (d.h. das beim Erzeugen uebergebene Bild) auf den per
     * Graphics-Objekt spezifizierten Bereich.
     * 
     * Wird eine andere Groesse als die des eigentlichen Bildes angegeben, so
     * wird das zugrunde liegende Bild einmalig skaliert und das neu erzeugte
     * Objekt gezeichnet. Dieser Ansatz ist dann effizient, wenn die Texturen
     * nur selten skaliert werden (davon kann bei einem Spiel ausgegangen
     * werden).
     * 
     * @param px X-Koordinate des linken oberen Pixels des Zielobjekts, ab dem
     * die Textur gezeichnet werden soll
     * @param py Y-Koordinate des linken oberen Pixels des Zielobjekts, ab dem
     * die Textur gezeichnet werden soll
     * @param width Breite des Bildes auf dem Zielobjekt
     * @param height Hoehe des Bildes auf dem Zielobjekt
     * @param g Zum Zielobjekt gehoeriges Graphics-Objekt
     */
    public void draw(int px, int py, int width, int height, Graphics2D g) {
        if (width < 0 && height < 0)
            draw(px, py, g);
        else if (width < 0)
            width = (this.width * height) / this.height;
        else if (height < 0)
            height = (this.height * width) / this.width;
        if (this.width != width || this.height != height)
            rescale(width, height);
        g.drawImage(scaledTexture, px, py, null);
    }

    public void draw(double posX, double posY, int width, int height,
            Graphics2D g) {
        draw((int) posX, (int) posY, width, height, g);
    }

    public void draw(int px, int py, Graphics2D g) {
        g.drawImage(scaledTexture, px, py, null);
    }

    public void drawCentered(int gWidth, int gHeight, Graphics2D g) {
        int px = (gWidth - this.width) / 2;
        int py = (gHeight - this.height) / 2;
        g.drawImage(scaledTexture, px, py, null);
    }

    public void drawCentered(int gWidth, int gHeight, int width, int height,
            Graphics2D g) {
        if (width < 0 && height < 0)
            drawCentered(gWidth, gHeight, g);
        else if (width < 0)
            width = (this.width * height) / this.height;
        else if (height < 0)
            height = (this.height * width) / this.width;
        if (this.width != width || this.height != height)
            rescale(width, height);
        drawCentered(gWidth, gHeight, g);
    }

    public void drawPartially(int posX, int posY, double part, Graphics2D g) {
        int width = (int) (scaledTexture.getWidth() * part);
        int height = scaledTexture.getHeight();
        g.drawImage(scaledTexture, posX, posY, posX + width, posY + height, 0,
                0, width, height, null);
    }

    private void rescale(int width, int height) {
        this.width = width;
        this.height = height;
        scaledTexture = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        scaledTexture
                .setAccelerationPriority(texture.getAccelerationPriority());
        Graphics2D g = (Graphics2D) scaledTexture.getGraphics();
        g.drawImage(texture, 0, 0, width, height, null);
        g.dispose();
    }

    public Texture mirrorHorizontally() {
        int width = this.texture.getWidth();
        int height = this.texture.getHeight();
        BufferedImage texture = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        texture.setAccelerationPriority(this.texture.getAccelerationPriority());
        Graphics2D g = (Graphics2D) texture.getGraphics();
        g.drawImage(this.texture, 0, 0, width, height, width, 0, 0, height,
                null);
        g.dispose();
        return new Texture(texture);
    }

    public Texture mirrorVertically() {
        int width = this.texture.getWidth();
        int height = this.texture.getHeight();
        BufferedImage texture = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        texture.setAccelerationPriority(this.texture.getAccelerationPriority());
        Graphics2D g = (Graphics2D) texture.getGraphics();
        g.drawImage(this.texture, 0, 0, width, height, 0, width, height, 0,
                null);
        g.dispose();
        return new Texture(texture);
    }

    public static final int darkenColor(int color) {
        int r, g, b;
        r = (color & 0xFF) - 128;
        g = ((color >> 8) & 0xFF) - 128;
        b = ((color >> 16) & 0xFF) - 128;
        if (b < 0)
            b = 0;
        if (g < 0)
            g = 0;
        if (r < 0)
            r = 0;
        return (color & 0xFF000000) | (b << 16) | (g << 8) | r;
    }

    public Texture replaceColors(int[] fromColors, int[] toColors) {
        int length = Math.min(fromColors.length, toColors.length);
        int width = this.texture.getWidth();
        int height = this.texture.getHeight();
        BufferedImage temp = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        temp.getGraphics().drawImage(texture, 0, 0, null);
        int[] rgb = new int[width * height];
        temp.getRGB(0, 0, width, height, rgb, 0, width);
        for (int i = 0; i < rgb.length; i++) {
            for (int j = 0; j < length; j++) {
                if ((rgb[i] & 0x00FFFFFF) == (fromColors[j] & 0x00FFFFFF)) {
                    rgb[i] = (rgb[i] & 0xFF000000) | (toColors[j] & 0x00FFFFFF);
                    break;
                }
            }
        }
        temp.setRGB(0, 0, width, height, rgb, 0, width);
        BufferedImage texture = new BufferedImage(width, height,
                BufferedImage.TYPE_4BYTE_ABGR);
        texture.setAccelerationPriority(this.texture.getAccelerationPriority());
        Graphics2D g = (Graphics2D) texture.getGraphics();
        g.drawImage(temp, 0, 0, width, height, null);
        return new Texture(texture);
    }

    public static Texture[] replaceColors(Texture[] textures, int[] fromColors,
            int[] toColors) {
        Texture[] newTextures = new Texture[textures.length];
        for (int i = 0; i < textures.length; i++)
            newTextures[i] = textures[i].replaceColors(fromColors, toColors);
        return newTextures;
    }

    public static Texture[][] replaceColors(Texture[][] textures,
            int[] fromColors, int[] toColors) {
        if (textures.length == 0)
            return null;
        Texture[][] newTextures = new Texture[textures.length][textures[1].length];
        for (int i = 0; i < textures.length; i++)
            for (int j = 0; j < textures[1].length; j++)
                newTextures[i][j] = textures[i][j].replaceColors(fromColors,
                        toColors);
        return newTextures;
    }

    /*************************************************************************
     ******************** Es folgen die Texturen *****************************
     *************************************************************************/

    /*
     * WICHTIGER TEIL HIER! Bisher werden die unten folgenden Texture aus der
     * Datei texture.png erzeugt. Hierueber können einfach andere Texturen
     * erzeugt werden, mit der folgenden Syntax:
     * 
     * public static final Texture *NAME* = new Texture(startXPixel,
     * startYPixel, Breite, Hoehe);
     * 
     * (startXPixel, startYPixel) ist hierbei der linke obere Pixel der
     * einzulesenden Textur in der Datei texture.png
     * 
     * texture.png befindet sich in Bombi/img/texture.png
     */

    public static Texture SKULL = new Texture(160, 128, 32, 32);

    private static Texture[] ALPHABET = getAlphabet();
    private static Texture[] NUMBERS = getNumbers();

    private static Texture[] getAlphabet() {
        Texture[] alphabet = new Texture[26];
        for (int i = 0; i < alphabet.length; i++)
            alphabet[i] = new Texture((i % 20) * 16, 224 + (16 * (i / 20)), 16,
                    16);
        return alphabet;
    }

    private static Texture[] getNumbers() {
        Texture[] numbers = new Texture[10];
        for (int i = 0; i < numbers.length; i++)
            numbers[i] = new Texture(96 + i * 16, 240, 16, 16);
        return numbers;
    }

    public static Texture drawString(String input) {
        String[] inputs = input.toLowerCase().split("\n");
        int width = inputs[0].length();
        for (int i = 1; i < inputs.length; i++)
            width = Math.max(width, inputs[i].length());
        BufferedImage text = new BufferedImage(width * 16, inputs.length * 16,
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D textg = (Graphics2D) text.getGraphics();
        for (int i = 0; i < inputs.length; i++) {
            int offset = (width - inputs[i].length()) / 2;
            for (int j = 0; j < inputs[i].length(); j++) {
                int chr = inputs[i].charAt(j);
                if (chr >= 'a' && chr <= 'z')
                    ALPHABET[chr - 'a'].draw((j + offset) * 16, i * 16, 16, 16,
                            textg);
                else if (chr >= '0' && chr <= '9')
                    NUMBERS[chr - '0'].draw((j + offset) * 16, i * 16, 16, 16,
                            textg);
            }
        }
        textg.dispose();
        return new Texture(text);
    }
} // end of class Texture
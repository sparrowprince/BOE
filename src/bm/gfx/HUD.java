package bm.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;

public class HUD implements Drawable {

    public static final Texture AMMO = new Texture(64, 192, 32, 32);
    public static final Texture RANGE = new Texture(96, 192, 32, 32);
    public static final Texture NORRIS = new Texture(128, 192, 32, 32);
    public static final Texture QUADDAMAGE = new Texture(160, 192, 32, 32);

    public static final HashMap<Byte, Texture> NUMBERS = new HashMap<Byte, Texture>();

    private FxPlayer player;
    private Texture name;
    private int dim;

    public HUD(FxPlayer player, int height) {
        this.player = player;
        name = Texture.drawString(player.getName()).replaceColors(
                Texture.TEXTCOLORS,
                new int[] { player.getFstClr(),
                        Texture.darkenColor(player.getFstClr()) });
        dim = height / 4;
    }

    @Override
    public void draw(Graphics2D g) {
        int offset = 1;

        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(0, 0, dim * 10, dim * 3, dim, dim);

        g.setColor(Color.LIGHT_GRAY);
        g.fillRoundRect(dim / 2, dim / 2, dim * 9, dim * 2, dim, dim);

        name.draw(dim * offset, dim / 2, -1, dim / 2, g);
        AMMO.draw(dim * offset, dim, dim, dim, g);
        offset++;
        getNumber(player.getAmmo()).draw(dim * offset, dim + 7, -1, dim / 2, g);
        offset++;
        RANGE.draw(dim * offset, dim, dim, dim, g);
        offset++;
        getNumber(player.getRange())
                .draw(dim * offset, dim + 7, -1, dim / 2, g);
        offset++;
        if (player.hasQuadDamage()) {
            QUADDAMAGE.draw(dim * offset, dim, dim, dim, g);
            offset++;
            getNumber((byte) (player.getQdCounter() / 60 + 1)).draw(
                    dim * offset, dim + 7, -1, dim / 2, g);
            offset++;
        }
        if (player.isChuck()) {
            NORRIS.draw(dim * offset, dim, dim, dim, g);
            offset++;
            getNumber((byte) (player.getCounter() / 60 + 1)).draw(dim * offset,
                    dim + 7, -1, dim / 2, g);
            offset++;
        } else if (player.isDead())
            Texture.SKULL.draw(dim * offset, dim, dim, dim, g);
    }

    private static Texture getNumber(byte number) {
        Texture texture = NUMBERS.get(number);
        if (texture == null) {
            texture = Texture.drawString(String.format("%2d", number));
            NUMBERS.put(number, texture);
        }
        return texture;
    }

    public void updateDimension(int height) {
        dim = height / 2;
    }
}

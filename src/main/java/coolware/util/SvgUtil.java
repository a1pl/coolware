package coolware.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SvgUtil {
    private static final Map<String, ResourceLocation> CACHED_TEXTURES = new HashMap<>();
    private static final Map<String, Path2D.Double> CACHED_PATHS = new HashMap<>();
    private static final Map<String, float[]> CACHED_VIEWBOXES = new HashMap<>();

    private static final Pattern VIEWBOX_PATTERN = Pattern.compile("viewBox=[\"']([0-9.\\s-]+)[\"']");
    private static final Pattern PATH_PATTERN = Pattern.compile("<path[^>]+d=[\"']([^\"']+)[\"']");

    private SvgUtil() {}

    public static void drawSvg(ResourceLocation svgLocation, float x, float y, float width, float height, int color) {
        ResourceLocation texture = getTexture(svgLocation, 256);
        if (texture == null) {
            String path = svgLocation.getResourcePath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.endsWith(".svg")) {
                name = name.substring(0, name.length() - 4) + ".png";
            }
            texture = new ResourceLocation(svgLocation.getResourceDomain(), "textures/sessioninfo/" + name);
        }
        RenderUtil.drawTexture(texture, x + 0.5F, y + 0.5F, width, height, 0x60000000);
        RenderUtil.drawTexture(texture, x, y, width, height, color);
    }

    public static void drawSvgBold(ResourceLocation svgLocation, float x, float y, float width, float height, int color) {
        drawSvg(svgLocation, x, y, width, height, color);
    }

    public static ResourceLocation getTexture(ResourceLocation svgLocation) {
        return getTexture(svgLocation, 256);
    }

    public static ResourceLocation getTexture(ResourceLocation svgLocation, int rasterSize) {
        if (svgLocation == null) return null;
        String key = svgLocation.toString() + "@" + rasterSize;
        if (CACHED_TEXTURES.containsKey(key)) {
            return CACHED_TEXTURES.get(key);
        }

        BufferedImage img = rasterizeSvg(svgLocation, rasterSize);
        if (img == null) {
            return null;
        }

        try {
            DynamicTexture dynamicTexture = new DynamicTexture(img);
            ResourceLocation dynLoc = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("svg_" + key.hashCode(), dynamicTexture);
            CACHED_TEXTURES.put(key, dynLoc);
            return dynLoc;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BufferedImage rasterizeSvg(ResourceLocation svgLocation, int targetSize) {
        Path2D.Double path = getPath(svgLocation);
        float[] viewBox = getViewBox(svgLocation);
        if (path == null || viewBox == null || viewBox[2] <= 0 || viewBox[3] <= 0) {
            return null;
        }

        BufferedImage img = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float scaleX = (float) targetSize / viewBox[2];
        float scaleY = (float) targetSize / viewBox[3];
        g.scale(scaleX, scaleY);
        g.translate(-viewBox[0], -viewBox[1]);

        g.setColor(Color.WHITE);
        g.fill(path);
        g.dispose();

        return img;
    }

    public static Path2D.Double getPath(ResourceLocation svgLocation) {
        String key = svgLocation.toString();
        if (CACHED_PATHS.containsKey(key)) {
            return CACHED_PATHS.get(key);
        }
        loadSvg(svgLocation);
        return CACHED_PATHS.get(key);
    }

    public static float[] getViewBox(ResourceLocation svgLocation) {
        String key = svgLocation.toString();
        if (CACHED_VIEWBOXES.containsKey(key)) {
            return CACHED_VIEWBOXES.get(key);
        }
        loadSvg(svgLocation);
        return CACHED_VIEWBOXES.get(key);
    }

    private static void loadSvg(ResourceLocation svgLocation) {
        String content = readSvgContent(svgLocation);
        if (content == null || content.isEmpty()) {
            return;
        }

        float[] vb = parseViewBox(content);
        List<String> dList = parsePathData(content);

        Path2D.Double composite = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        for (String d : dList) {
            Path2D.Double subPath = parsePath(d);
            composite.append(subPath, false);
        }

        String key = svgLocation.toString();
        CACHED_VIEWBOXES.put(key, vb);
        CACHED_PATHS.put(key, composite);
    }

    private static String readSvgContent(ResourceLocation location) {
        // 1. Try Minecraft Resource Manager
        try {
            IResource res = Minecraft.getMinecraft().getResourceManager().getResource(location);
            if (res != null) {
                try (InputStream in = res.getInputStream()) {
                    return readString(in);
                }
            }
        } catch (Throwable ignored) {}

        // 2. Try ClassLoader with domain
        try {
            String path = "/assets/" + location.getResourceDomain() + "/" + location.getResourcePath();
            try (InputStream in = SvgUtil.class.getResourceAsStream(path)) {
                if (in != null) return readString(in);
            }
        } catch (Throwable ignored) {}

        // 3. Try direct resource path
        try {
            String path = "/" + location.getResourcePath();
            try (InputStream in = SvgUtil.class.getResourceAsStream(path)) {
                if (in != null) return readString(in);
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private static String readString(InputStream in) throws Exception {
        byte[] buffer = new byte[8192];
        StringBuilder sb = new StringBuilder();
        int read;
        while ((read = in.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static float[] parseViewBox(String svg) {
        Matcher m = VIEWBOX_PATTERN.matcher(svg);
        if (m.find()) {
            String[] parts = m.group(1).trim().split("[\\s,]+");
            if (parts.length >= 4) {
                try {
                    return new float[]{
                            Float.parseFloat(parts[0]),
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3])
                    };
                } catch (Exception ignored) {}
            }
        }
        return new float[]{0, 0, 512, 512};
    }

    private static List<String> parsePathData(String svg) {
        List<String> list = new ArrayList<>();
        Matcher m = PATH_PATTERN.matcher(svg);
        while (m.find()) {
            list.add(m.group(1));
        }
        return list;
    }

    public static Path2D.Double parsePath(String d) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        double curX = 0, curY = 0;
        double startX = 0, startY = 0;
        double lastCtrlX = 0, lastCtrlY = 0;
        char lastCmd = ' ';

        int idx = 0;
        int len = d.length();

        while (idx < len) {
            while (idx < len && (Character.isWhitespace(d.charAt(idx)) || d.charAt(idx) == ',')) {
                idx++;
            }
            if (idx >= len) break;

            char c = d.charAt(idx);
            char cmd = c;
            if (Character.isLetter(c)) {
                idx++;
            } else {
                cmd = lastCmd;
            }

            switch (cmd) {
                case 'M':
                case 'm': {
                    double x = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    if (cmd == 'm') {
                        x += curX;
                        y += curY;
                    }
                    path.moveTo(x, y);
                    curX = x; curY = y;
                    startX = x; startY = y;
                    lastCtrlX = x; lastCtrlY = y;
                    lastCmd = cmd == 'M' ? 'L' : 'l';
                    break;
                }
                case 'L':
                case 'l': {
                    double x = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    if (cmd == 'l') {
                        x += curX;
                        y += curY;
                    }
                    path.lineTo(x, y);
                    curX = x; curY = y;
                    lastCtrlX = x; lastCtrlY = y;
                    lastCmd = cmd;
                    break;
                }
                case 'H':
                case 'h': {
                    double x = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    if (cmd == 'h') x += curX;
                    path.lineTo(x, curY);
                    curX = x;
                    lastCtrlX = curX; lastCtrlY = curY;
                    lastCmd = cmd;
                    break;
                }
                case 'V':
                case 'v': {
                    double y = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    if (cmd == 'v') y += curY;
                    path.lineTo(curX, y);
                    curY = y;
                    lastCtrlX = curX; lastCtrlY = curY;
                    lastCmd = cmd;
                    break;
                }
                case 'C':
                case 'c': {
                    double x1 = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y1 = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double x2 = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y2 = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double x = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    if (cmd == 'c') {
                        x1 += curX; y1 += curY;
                        x2 += curX; y2 += curY;
                        x += curX; y += curY;
                    }
                    path.curveTo(x1, y1, x2, y2, x, y);
                    curX = x; curY = y;
                    lastCtrlX = x2; lastCtrlY = y2;
                    lastCmd = cmd;
                    break;
                }
                case 'S':
                case 's': {
                    double x2 = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y2 = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double x = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    if (cmd == 's') {
                        x2 += curX; y2 += curY;
                        x += curX; y += curY;
                    }
                    double x1 = curX;
                    double y1 = curY;
                    if (lastCmd == 'C' || lastCmd == 'c' || lastCmd == 'S' || lastCmd == 's') {
                        x1 = 2 * curX - lastCtrlX;
                        y1 = 2 * curY - lastCtrlY;
                    }
                    path.curveTo(x1, y1, x2, y2, x, y);
                    curX = x; curY = y;
                    lastCtrlX = x2; lastCtrlY = y2;
                    lastCmd = cmd;
                    break;
                }
                case 'A':
                case 'a': {
                    double rx = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double ry = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double rot = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double largeArc = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double sweep = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double x = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    double y = nextNumber(d, idx); idx = nextNumberEnd(d, idx);
                    if (cmd == 'a') {
                        x += curX;
                        y += curY;
                    }
                    arcTo(path, curX, curY, rx, ry, rot, largeArc != 0, sweep != 0, x, y);
                    curX = x; curY = y;
                    lastCtrlX = x; lastCtrlY = y;
                    lastCmd = cmd;
                    break;
                }
                case 'Z':
                case 'z': {
                    path.closePath();
                    curX = startX; curY = startY;
                    lastCtrlX = curX; lastCtrlY = curY;
                    lastCmd = cmd;
                    break;
                }
                default:
                    idx++;
                    break;
            }
        }
        return path;
    }

    private static double nextNumber(String s, int start) {
        int idx = start;
        int len = s.length();
        while (idx < len && (Character.isWhitespace(s.charAt(idx)) || s.charAt(idx) == ',')) idx++;
        int begin = idx;
        if (idx < len && (s.charAt(idx) == '+' || s.charAt(idx) == '-')) idx++;
        while (idx < len && (Character.isDigit(s.charAt(idx)) || s.charAt(idx) == '.')) idx++;
        if (idx < len && (s.charAt(idx) == 'e' || s.charAt(idx) == 'E')) {
            idx++;
            if (idx < len && (s.charAt(idx) == '+' || s.charAt(idx) == '-')) idx++;
            while (idx < len && Character.isDigit(s.charAt(idx))) idx++;
        }
        if (begin >= idx) return 0;
        return Double.parseDouble(s.substring(begin, idx));
    }

    private static int nextNumberEnd(String s, int start) {
        int idx = start;
        int len = s.length();
        while (idx < len && (Character.isWhitespace(s.charAt(idx)) || s.charAt(idx) == ',')) idx++;
        if (idx < len && (s.charAt(idx) == '+' || s.charAt(idx) == '-')) idx++;
        while (idx < len && (Character.isDigit(s.charAt(idx)) || s.charAt(idx) == '.')) idx++;
        if (idx < len && (s.charAt(idx) == 'e' || s.charAt(idx) == 'E')) {
            idx++;
            if (idx < len && (s.charAt(idx) == '+' || s.charAt(idx) == '-')) idx++;
            while (idx < len && Character.isDigit(s.charAt(idx))) idx++;
        }
        return idx;
    }

    private static void arcTo(Path2D.Double path, double x0, double y0, double rx, double ry,
                              double angle, boolean largeArc, boolean sweep, double x, double y) {
        if (rx == 0 || ry == 0) {
            path.lineTo(x, y);
            return;
        }
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        double rad = Math.toRadians(angle % 360.0);
        double cosAngle = Math.cos(rad);
        double sinAngle = Math.sin(rad);

        double dx2 = (x0 - x) / 2.0;
        double dy2 = (y0 - y) / 2.0;
        double x1 = cosAngle * dx2 + sinAngle * dy2;
        double y1 = -sinAngle * dx2 + cosAngle * dy2;

        double prx = rx * rx;
        double pry = ry * ry;
        double px1 = x1 * x1;
        double py1 = y1 * y1;

        double check = px1 / prx + py1 / pry;
        if (check > 1) {
            rx *= Math.sqrt(check);
            ry *= Math.sqrt(check);
            prx = rx * rx;
            pry = ry * ry;
        }

        double sign = (largeArc == sweep) ? -1 : 1;
        double num = prx * pry - prx * py1 - pry * px1;
        double denom = prx * py1 + pry * px1;
        double coef = sign * Math.sqrt(Math.max(0, num / denom));
        double cx1 = coef * ((rx * y1) / ry);
        double cy1 = coef * -((ry * x1) / rx);

        double cx = cosAngle * cx1 - sinAngle * cy1 + (x0 + x) / 2.0;
        double cy = sinAngle * cx1 + cosAngle * cy1 + (y0 + y) / 2.0;

        double theta1 = angleBetween(1, 0, (x1 - cx1) / rx, (y1 - cy1) / ry);
        double dTheta = angleBetween((x1 - cx1) / rx, (y1 - cy1) / ry, (-x1 - cx1) / rx, (-y1 - cy1) / ry);

        if (!sweep && dTheta > 0) dTheta -= 2 * Math.PI;
        else if (sweep && dTheta < 0) dTheta += 2 * Math.PI;

        int segments = (int) Math.ceil(Math.abs(dTheta) / (Math.PI / 2.0));
        double delta = dTheta / segments;
        double t = 8.0 / 3.0 * Math.sin(delta / 4.0) * Math.sin(delta / 4.0) / Math.sin(delta / 2.0);

        double startAngle = theta1;
        for (int i = 0; i < segments; i++) {
            double endAngle = startAngle + delta;
            double cosStart = Math.cos(startAngle);
            double sinStart = Math.sin(startAngle);
            double cosEnd = Math.cos(endAngle);
            double sinEnd = Math.sin(endAngle);

            double cpX1 = cx + rx * (cosStart - t * sinStart);
            double cpY1 = cy + ry * (sinStart + t * cosStart);
            double cpX2 = cx + rx * (cosEnd + t * sinEnd);
            double cpY2 = cy + ry * (sinEnd - t * cosEnd);
            double epX2 = cx + rx * cosEnd;
            double epY2 = cy + ry * sinEnd;

            path.curveTo(cpX1, cpY1, cpX2, cpY2, epX2, epY2);
            startAngle = endAngle;
        }
    }

    private static double angleBetween(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dot / len)));
        if (ux * vy - uy * vx < 0) angle = -angle;
        return angle;
    }
}

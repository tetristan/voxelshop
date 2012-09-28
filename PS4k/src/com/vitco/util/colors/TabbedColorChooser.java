package com.vitco.util.colors;

import com.jidesoft.swing.JideTabbedPane;
import com.vitco.util.ColorTools;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced color chooser that uses tabs to display different
 * ways of altering the color.
 */
public class TabbedColorChooser extends ColorChooserPrototype {

    private static final Color TEXTAREA_BG_COLOR = new Color(60, 60, 60);
    private static final Color TEXTAREA_BG_COLOR_FOCUS = new Color(200, 200, 200);
    private static final Color TEXTAREA_TEXT_COLOR = new Color(255, 255, 255);
    private static final Color TEXTAREA_TEXT_COLOR_FOCUS = new Color(0, 0, 0);
    private static final Color TEXTAREA_BORDER_COLOR = new Color(50, 50, 50);
    private static final Color BG_COLOR = new Color(80, 80, 80);
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final Color SLIDER_BORDER_COLOR = new Color(50, 50, 50);
    private static final Color SLIDER_KNOB_COLOR = new Color(158, 158, 158);
    private static final Color SLIDER_KNOB_OUTLINE_COLOR = new Color(42, 42, 42);

    // ======================

    // box that only allows numbers and has an easy way to retrieve the current one
    // also allows for notification listen (and change "onChange")
    private static class NumberBox extends JTextField {

        // make sure the value is only set when it changes
        public void setValueWithoutRefresh(int value) {
            int croppedValue = cropValue(value);
            if (getValue() != croppedValue) {
                blockNotify = true;
                super.setText(String.valueOf(croppedValue));
                blockNotify = false;
            }
        }

        // the current string used
        private String currentString = "";

        // holds the listeners
        private final ArrayList<TextChangeListener> listener = new ArrayList<NumberBox.TextChangeListener>();
        // the listener interface
        protected interface TextChangeListener {
            void onChange();
        }

        // add a listener
        public final void addTextChangeListener(TextChangeListener tcl) {
            listener.add(tcl);
        }

        // notify listeners
        private boolean blockNotify = false;
        private void notifyListeners() {
            if (!blockNotify) {
                for (TextChangeListener tcl : listener) {
                    tcl.onChange();
                }
            }
        }

        // filter to allow only numbers in textarea and notify on change
        // also remembers the current string
        private class AxisJTextFilter extends DocumentFilter {
            @Override
            public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException
            {
                StringBuilder sb = new StringBuilder();
                sb.append(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.insert(offset, text);
                if(invalidContent(sb.toString())) return;
                fb.insertString(offset, text, attr);
                currentString = sb.toString();
                notifyListeners();
            }

            @Override
            public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attr) throws BadLocationException
            {
                StringBuilder sb = new StringBuilder();
                sb.append(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.replace(offset, offset + length, text);
                if(invalidContent(sb.toString())) return;
                fb.replace(offset, length, text, attr);
                currentString = sb.toString();
                notifyListeners();
            }

            @Override
            public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws BadLocationException
            {
                super.remove(fb, offset, length);
                currentString = fb.getDocument().getText(0, fb.getDocument().getLength());
                notifyListeners();
            }

            public boolean invalidContent(String text)
            {
                Pattern pattern = Pattern.compile("\\d{0,4}?");
                Matcher matcher = pattern.matcher(text);
                boolean isMatch = matcher.matches();
                return !text.equals("") && !isMatch;
            }
        }

        // link to this field
        private final NumberBox thisField = this;

        // conversion helper
        private int cropValue(int value)  {
            return Math.min(MAX,Math.max(MIN,value));
        }

        // get value (range 0-255)
        public final int getValue() {
            int result = 0;
            try {
                result = cropValue(Integer.valueOf(currentString));
            } catch (NumberFormatException ignored) {}
            return result;
        }

        private final int MIN;
        private final int MAX;

        public NumberBox(int min, int max, int current) {
            super(String.valueOf(current), 4);
            MIN = min;
            MAX = max;
            setForeground(TEXTAREA_TEXT_COLOR);
            setBackground(TEXTAREA_BG_COLOR);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(TEXTAREA_BORDER_COLOR),
                    BorderFactory.createEmptyBorder(0, 3, 0, 3)
            ));
            ((AbstractDocument)this.getDocument()).setDocumentFilter(new AxisJTextFilter());
            // handle highlight on focus
            this.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    setForeground(TEXTAREA_TEXT_COLOR_FOCUS);
                    setBackground(TEXTAREA_BG_COLOR_FOCUS);
                }

                @Override
                public void focusLost(FocusEvent e) {
                    thisField.setText(String.valueOf(getValue()));
                    setForeground(TEXTAREA_TEXT_COLOR);
                    setBackground(TEXTAREA_BG_COLOR);
                }
            });
            // handle update on return (only visual)
            this.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == 10) {
                        thisField.setText(String.valueOf(getValue()));
                        // remove focus from this component
                        thisField.setFocusable(false);
                        thisField.setFocusable(true);
                    }
                }
            });
        }
    }

    // ======================

    // custom slider
    private static abstract class HorizontalSliderPrototype extends JSlider {

        // holds the listeners
        private final ArrayList<ValueChangeListener> listener = new ArrayList<ValueChangeListener>();
        // the listener interface
        protected interface ValueChangeListener {
            void onChange(ChangeEvent e);
        }

        // add a listener
        public final void addValueChangeListener(ValueChangeListener vcl) {
            listener.add(vcl);
        }

        // notify listeners
        private boolean blockNotify = false;
        private void notifyListeners() {
            if (!blockNotify) {
                for (ValueChangeListener vcl : listener) {
                    vcl.onChange(this.changeEvent);
                }
            }
        }

        public final void setValueWithoutRefresh(int value) {
            if (getValue() != value) {
                blockNotify = true;
                setValue(value);
                blockNotify = false;
            }
        }

        // custom slider ui
        protected class SliderUI extends BasicSliderUI {
            public SliderUI(JSlider b) {
                super(b);
            }

            public final JSlider getSlider() {
                return slider;
            }

            public final Rectangle getContentRect() {
                return contentRect;
            }

            public final int getXPositionForValue(int value) {
                return xPositionForValue(value);
            }

            public final int getValueForXPosition(int value) {
                return valueForXPosition(value);
            }
        }

        abstract void drawBackground(Graphics2D g, SliderUI sliderUI);

        // constructor
        public HorizontalSliderPrototype(int min, int max, int current) {
            super(JSlider.HORIZONTAL, min, max, current);
            setPreferredSize(new Dimension(150, 20));

            // notification
            addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });

            // ================
            // create thumb
            final int size = 5;
            final BufferedImage thumbBuffer = new BufferedImage(size*2 + 1, size*2 + 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = (Graphics2D) thumbBuffer.getGraphics();
            // Anti-alias
            ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            ig.setColor(SLIDER_KNOB_COLOR);
            ig.fillPolygon(new int[] {1,size*2,size*2,size,1}, new int[] {size*2, size*2, size, 1, size}, 5);

            ig.setColor(SLIDER_KNOB_OUTLINE_COLOR);
            ig.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL)); // line size
            ig.drawPolygon(new int[] {0,size*2,size*2,size,0}, new int[] {size*2, size*2, size, 0, size}, 5);
            // ===============

            // create ui
            final SliderUI sliderUI = new SliderUI(this) {
                @Override
                public void paint(Graphics g, JComponent c) {
                    super.paint(g, c);

                    // draw the background
                    drawBackground((Graphics2D)g, this);

                    if (g.getClipBounds().intersects(thumbRect)) {
                        // make sure the thumbRect covers the whole height
                        thumbRect.y = 0;
                        thumbRect.height = slider.getHeight();
                        // draw the thumb
                        g.drawImage(thumbBuffer, xPositionForValue(slider.getValue()) - size, slider.getHeight()-size*2 - 1, null);
                    }
                }
            };
            // move the thumb to the position we pressed instantly
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setValue(sliderUI.valueForXPosition(e.getX()));
                    repaint();
                }
            });
            // register - needs to go last
            setUI(sliderUI);
        }
    }

    // custom slider
    private static class HorizontalColorSlider extends HorizontalSliderPrototype {

        // the left color of the slider
        private Color leftColor = Color.WHITE;
        public final void setLeftColor(Color color) {
            leftColor = color;
        }

        // the right color of the slider
        private Color rightColor = Color.BLACK;
        public final void setRightColor(Color color) {
            rightColor = color;
        }

        private BufferedImage bgBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        private Point prevContentRect = new Point(0, 0);
        private Color lastLeftColor = Color.BLACK;
        private Color lastRightColor = Color.WHITE;

        @Override
        void drawBackground(Graphics2D g, SliderUI sliderUI) {
            // only generate background on resize and when color changes
            if (prevContentRect.x != sliderUI.getContentRect().width || prevContentRect.y != sliderUI.getContentRect().height ||
                    !lastLeftColor.equals(leftColor) || !lastRightColor.equals(rightColor)) {

                lastLeftColor = leftColor;
                lastRightColor = rightColor;

                prevContentRect = new Point(sliderUI.getContentRect().width, sliderUI.getContentRect().height);
                int w = sliderUI.getSlider().getWidth();
                int h = sliderUI.getSlider().getHeight();
                bgBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D ig = (Graphics2D) bgBuffer.getGraphics();

                ig.setColor(BG_COLOR);
                ig.fillRect(0, 0, w, h);

                // leave some free for the slider
                ig.setPaint(new GradientPaint(sliderUI.getXPositionForValue(0), 0, leftColor, sliderUI.getXPositionForValue(255), 0, rightColor, false));
                ig.fillRect(1, 1, w - 2, h - 11);
                ig.setColor(SLIDER_BORDER_COLOR);
                ig.drawRect(0, 0, w - 1, h - 10);
            }

            // draw the background
            g.drawImage(bgBuffer, 0, 0, null);
        }

        // constructor
        public HorizontalColorSlider(int min, int max, int current) {
            super(min, max, current);
        }
    }

    // hue slider
    private static class HorizontalHueSlider extends HorizontalSliderPrototype {

        private Point prevContentRect = new Point(0, 0);
        private BufferedImage bgBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

        @Override
        void drawBackground(Graphics2D g, SliderUI sliderUI) {
            // only generate background on resize and when color changes
            if (prevContentRect.x != sliderUI.getContentRect().width || prevContentRect.y != sliderUI.getContentRect().height) {

                prevContentRect = new Point(sliderUI.getContentRect().width, sliderUI.getContentRect().height);
                int w = sliderUI.getSlider().getWidth();
                int h = sliderUI.getSlider().getHeight();
                bgBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D ig = (Graphics2D) bgBuffer.getGraphics();

                ig.setColor(BG_COLOR);
                ig.fillRect(0, 0, w, h);

                // leave some free for the slider
                for (int x = 0; x < w - 1; x++) {
                    ig.setColor(ColorTools.hsbToColor(new float[] {(float)sliderUI.getValueForXPosition(x)/ sliderUI.getSlider().getMaximum(), 1, 1}));
                    ig.drawLine(x, 1, x, h - 11);
                }
                ig.setColor(SLIDER_BORDER_COLOR);
                ig.drawRect(0, 0, w - 1, h - 10);
            }

            // draw the background
            g.drawImage(bgBuffer, 0, 0, null);
        }

        // constructor
        public HorizontalHueSlider(int min, int max, int current) {
            super(min, max, current);
        }
    }

    // ======================

    // tab prototype
    private abstract class TabPrototype extends JPanel {
        private HorizontalSliderPrototype[] sliders;
        private NumberBox[] fields;

        protected abstract void onSliderChange(int id, ChangeEvent e);
        protected abstract void onTextFieldChange(int id, NumberBox source);
        protected abstract void refreshUI();
        protected abstract void notifyColorChange(Color color);

        private boolean hasChanged = false;
        protected void update(Color newColor, boolean externalChange, boolean publishIfChanged) {
            boolean changed = !color.equals(newColor);
            if (changed || externalChange) {
                // set the color
                color = newColor;
                if (changed) {
                    hasChanged = true;
                }
                if (externalChange) {
                    notifyColorChange(newColor);
                }
                refreshUI();
            }
            // notify the listeners
            if (hasChanged && publishIfChanged) {
                hasChanged = false;
                notifyListeners(color);
            }
        }

        // update displayed values
        protected final void setValues(int[] values) {
            for (int i = 0; i < values.length; i++) {
                sliders[i].setValueWithoutRefresh(values[i]);
                fields[i].setValueWithoutRefresh(values[i]);
            }
        }

        protected final void init(String[] values, HorizontalSliderPrototype[] sliders, final NumberBox[] fields) {
            // store internal
            this.sliders = sliders;
            this.fields = fields;

            // align only when showing
            addHierarchyListener(new HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                        if (isShowing()) {
                            update(color, true, false);
                        }
                    }
                }
            });

            // register slider events
            for (int id = 0; id < sliders.length; id++) {
                final int finalId = id;
                sliders[id].addValueChangeListener(new HorizontalSliderPrototype.ValueChangeListener() {
                    @Override
                    public void onChange(ChangeEvent e) {
                        onSliderChange(finalId, e);
                    }
                });
            }

            // register textfield events
            for (int id = 0; id < fields.length; id++) {
                final int finalId = id;
                fields[id].addTextChangeListener(new NumberBox.TextChangeListener() {
                    @Override
                    public void onChange() {
                        onTextFieldChange(finalId, fields[finalId]);
                    }
                });
            }

            // construct the layout
            setLayout(new GridBagLayout());
            setBackground(BG_COLOR);
            final GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(3,3,3,3);
            c.fill = GridBagConstraints.BOTH;

            // labels
            c.gridx = 0;
            c.gridy = 0;
            for (String value : values) {
                JLabel label = new JLabel(value);
                label.setForeground(TEXT_COLOR);
                add(label, c);
                c.gridy++;
            }

            // slider
            c.gridx = 1;
            c.gridy = 0;
            for (HorizontalSliderPrototype slider : sliders) {
                add(slider, c);
                c.gridy++;
            }

            // text fields
            c.gridx = 2;
            c.gridy = 0;
            for (NumberBox field : fields) {
                add(field, c);
                c.gridy++;
            }
        }
    }

    // the rgb chooser
    private final RGBTab rgbTab = new RGBTab();
    private class RGBTab extends TabPrototype {

        // the sliders
        private final HorizontalColorSlider rSlider = new HorizontalColorSlider(0, 255, 0);
        private final HorizontalColorSlider gSlider = new HorizontalColorSlider(0, 255, 0);
        private final HorizontalColorSlider bSlider = new HorizontalColorSlider(0, 255, 0);

        // the number boxes
        private final NumberBox rBox = new NumberBox(0, 255, 0);
        private final NumberBox gBox = new NumberBox(0, 255, 0);
        private final NumberBox bBox = new NumberBox(0, 255, 0);

        public RGBTab() {

            init(
                    new String[]{"R", "G", "B"},
                    new HorizontalSliderPrototype[]{rSlider, gSlider, bSlider},
                    new NumberBox[]{rBox, gBox, bBox}
            );
        }

        @Override
        protected void onSliderChange(int id, ChangeEvent e) {
            JSlider source = ((JSlider) e.getSource());
            update(new Color(
                    id == 0 ? source.getValue() : color.getRed(),
                    id == 1 ? source.getValue() : color.getGreen(),
                    id == 2 ? source.getValue() : color.getBlue()),
                    false, !source.getValueIsAdjusting());
        }

        @Override
        protected void onTextFieldChange(int id, NumberBox source) {
            update(new Color(
                    id == 0 ? source.getValue() : color.getRed(),
                    id == 1 ? source.getValue() : color.getGreen(),
                    id == 2 ? source.getValue() : color.getBlue()),
                    false, true);
        }

        @Override
        protected void refreshUI() {
            // repaint the slider
            rSlider.setLeftColor(new Color(0, color.getGreen(), color.getBlue()));
            rSlider.setRightColor(new Color(255, color.getGreen(), color.getBlue()));
            rSlider.repaint();

            gSlider.setLeftColor(new Color(color.getRed(), 0, color.getBlue()));
            gSlider.setRightColor(new Color(color.getRed(), 255, color.getBlue()));
            gSlider.repaint();

            bSlider.setLeftColor(new Color(color.getRed(), color.getGreen(), 0));
            bSlider.setRightColor(new Color(color.getRed(), color.getGreen(), 255));
            bSlider.repaint();

            // set the values
            setValues(new int[] {color.getRed(), color.getGreen(), color.getBlue()});
        }

        @Override
        protected void notifyColorChange(Color color) {
            // nothing to do here
        }
    }

    // the hsb chooser
    private final HSBTab hsbTab = new HSBTab();
    private class HSBTab extends TabPrototype {

        private final static int HUE_STEPCOUNT = 360;
        private final static int STEPCOUNT = 100;

        // the sliders
        private final TabbedColorChooser.HorizontalHueSlider hSlider = new TabbedColorChooser.HorizontalHueSlider(0, HUE_STEPCOUNT, 0);
        private final TabbedColorChooser.HorizontalColorSlider sSlider = new TabbedColorChooser.HorizontalColorSlider(0, STEPCOUNT, 0);
        private final TabbedColorChooser.HorizontalColorSlider bSlider = new TabbedColorChooser.HorizontalColorSlider(0, STEPCOUNT, 0);

        // the number boxes
        private final NumberBox hBox = new NumberBox(0, HUE_STEPCOUNT, 0);
        private final NumberBox sBox = new NumberBox(0, STEPCOUNT, 0);
        private final NumberBox bBox = new NumberBox(0, STEPCOUNT, 0);

        public HSBTab() {
            init(
                    new String[]{"H", "S", "B"},
                    new HorizontalSliderPrototype[]{hSlider, sSlider, bSlider},
                    new NumberBox[]{hBox, sBox, bBox}
            );
        }

        @Override
        protected void onSliderChange(int id, ChangeEvent e) {
            JSlider source = ((JSlider) e.getSource());
            hsb = new float[]{
                    id == 0 ? (float) source.getValue() / HUE_STEPCOUNT : hsb[0],
                    id == 1 ? (float) source.getValue() / STEPCOUNT : hsb[1],
                    id == 2 ? (float) source.getValue() / STEPCOUNT : hsb[2]};
            update(ColorTools.hsbToColor(hsb), false, !source.getValueIsAdjusting());
        }

        @Override
        protected void onTextFieldChange(int id, NumberBox source) {
            hsb = new float[]{
                    id == 0 ? (float) source.getValue() / HUE_STEPCOUNT : hsb[0],
                    id == 1 ? (float) source.getValue() / STEPCOUNT : hsb[1],
                    id == 2 ? (float) source.getValue() / STEPCOUNT : hsb[2]};
            update(ColorTools.hsbToColor(hsb), false, true);
        }

        @Override
        protected void refreshUI() {
            sSlider.setLeftColor(ColorTools.hsbToColor(new float[] {hsb[0], 0, hsb[2]}));
            sSlider.setRightColor(ColorTools.hsbToColor(new float[] {hsb[0], 1, hsb[2]}));
            sSlider.repaint();

            bSlider.setLeftColor(ColorTools.hsbToColor(new float[] {hsb[0], hsb[1], 0}));
            bSlider.setRightColor(ColorTools.hsbToColor(new float[] {hsb[0], hsb[1], 1}));
            bSlider.repaint();

            // set the values
            setValues(new int[] {
                    Math.round(hsb[0] * HUE_STEPCOUNT),
                    Math.round(hsb[1] * STEPCOUNT),
                    Math.round(hsb[2] * STEPCOUNT)
            });
        }

        private float[] hsb = new float[3];

        @Override
        protected void notifyColorChange(Color color) {
            hsb = ColorTools.colorToHSB(color);
        }
    }

    private final CMYKTab cmykTab = new CMYKTab();
    private class CMYKTab extends TabPrototype {

        private final static int STEPCOUNT = 100;

        // the sliders
        private final TabbedColorChooser.HorizontalColorSlider cSlider = new TabbedColorChooser.HorizontalColorSlider(0, STEPCOUNT, 0);
        private final TabbedColorChooser.HorizontalColorSlider mSlider = new TabbedColorChooser.HorizontalColorSlider(0, STEPCOUNT, 0);
        private final TabbedColorChooser.HorizontalColorSlider ySlider = new TabbedColorChooser.HorizontalColorSlider(0, STEPCOUNT, 0);
        private final TabbedColorChooser.HorizontalColorSlider kSlider = new TabbedColorChooser.HorizontalColorSlider(0, STEPCOUNT, 0);

        // the number boxes
        private final NumberBox cBox = new NumberBox(0, STEPCOUNT, 0);
        private final NumberBox mBox = new NumberBox(0, STEPCOUNT, 0);
        private final NumberBox yBox = new NumberBox(0, STEPCOUNT, 0);
        private final NumberBox kBox = new NumberBox(0, STEPCOUNT, 0);

        public CMYKTab() {
            init(
                    new String[]{"C", "M", "Y", "K"},
                    new HorizontalSliderPrototype[]{cSlider, mSlider, ySlider, kSlider},
                    new NumberBox[]{cBox, mBox, yBox, kBox}
            );
        }

        @Override
        protected void onSliderChange(int id, ChangeEvent e) {
            JSlider source = ((JSlider) e.getSource());
            cmyk = new float[]{
                    id == 0 ? (float) source.getValue() / STEPCOUNT : cmyk[0],
                    id == 1 ? (float) source.getValue() / STEPCOUNT : cmyk[1],
                    id == 2 ? (float) source.getValue() / STEPCOUNT : cmyk[2],
                    id == 3 ? (float) source.getValue() / STEPCOUNT : cmyk[3]};
            update(ColorTools.cmykToColor(cmyk), false, !source.getValueIsAdjusting());
        }

        @Override
        protected void onTextFieldChange(int id, NumberBox source) {
            cmyk = new float[]{
                    id == 0 ? (float) source.getValue() / STEPCOUNT : cmyk[0],
                    id == 1 ? (float) source.getValue() / STEPCOUNT : cmyk[1],
                    id == 2 ? (float) source.getValue() / STEPCOUNT : cmyk[2],
                    id == 3 ? (float) source.getValue() / STEPCOUNT : cmyk[3]};
            update(ColorTools.cmykToColor(cmyk), false, true);
        }

        @Override
        protected void refreshUI() {
            cSlider.setLeftColor(ColorTools.cmykToColor(new float[]{0, cmyk[1], cmyk[2], cmyk[3]}));
            cSlider.setRightColor(ColorTools.cmykToColor(new float[]{1, cmyk[1], cmyk[2], cmyk[3]}));
            cSlider.repaint();

            mSlider.setLeftColor(ColorTools.cmykToColor(new float[]{cmyk[0], 0, cmyk[2], cmyk[3]}));
            mSlider.setRightColor(ColorTools.cmykToColor(new float[]{cmyk[0], 1, cmyk[2], cmyk[3]}));
            mSlider.repaint();

            ySlider.setLeftColor(ColorTools.cmykToColor(new float[]{cmyk[0], cmyk[1], 0, cmyk[3]}));
            ySlider.setRightColor(ColorTools.cmykToColor(new float[]{cmyk[0], cmyk[1], 1, cmyk[3]}));
            ySlider.repaint();

            kSlider.setLeftColor(ColorTools.cmykToColor(new float[]{cmyk[0], cmyk[1], cmyk[2], 0}));
            kSlider.setRightColor(ColorTools.cmykToColor(new float[]{cmyk[0], cmyk[1], cmyk[2], 1}));
            kSlider.repaint();

            // set the values
            setValues(new int[] {
                    Math.round(cmyk[0] * STEPCOUNT),
                    Math.round(cmyk[1] * STEPCOUNT),
                    Math.round(cmyk[2] * STEPCOUNT),
                    Math.round(cmyk[3] * STEPCOUNT)
            });
        }

        private float[] cmyk = new float[4];

        @Override
        protected void notifyColorChange(Color color) {
            cmyk = ColorTools.colorToCMYK(color);
        }
    }

    // the tabbed pane
    private final JideTabbedPane tabbedPane = new JideTabbedPane(JTabbedPane.RIGHT, JideTabbedPane.SCROLL_TAB_LAYOUT);

    // ======================

    // get the active tab
    public final int getActiveTab() {
        return tabbedPane.getSelectedIndex();
    }

    // set the active tab
    public final void setActiveTab(int selectedIndex) {
        if (tabbedPane.getTabCount() > selectedIndex && selectedIndex >= 0) {
            tabbedPane.setSelectedIndex(selectedIndex);
        }
    }

    // set the color that is currently displayed
    private Color color = Color.WHITE;
    public final void setColor(float[] hsb) {
        Color color = ColorTools.hsbToColor(hsb);
        if (!this.color.equals(color)) {
            if (rgbTab.isShowing()) {
                rgbTab.update(color, true, false);
            }
            if (hsbTab.isShowing()) {
                hsbTab.update(color, true, false);
            }
            if (cmykTab.isShowing()) {
                cmykTab.update(color, true, false);
            }
        }
    }

    // constructor
    public TabbedColorChooser() {

        // set up the tabbed pane
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder());
        setBackground(new Color(0, 0, 0, 0));

        // add the tabs
        tabbedPane.addTab("RGB", new JScrollPane(rgbTab));
        tabbedPane.addTab("HSB", new JScrollPane(hsbTab));
        tabbedPane.addTab("CMYK", new JScrollPane(cmykTab));

        tabbedPane.setTabShape(JideTabbedPane.SHAPE_ROUNDED_FLAT); // make square
        tabbedPane.setTabResizeMode(JideTabbedPane.RESIZE_MODE_FIT);

        add(tabbedPane, BorderLayout.CENTER);
    }
}

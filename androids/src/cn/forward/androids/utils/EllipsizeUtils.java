package cn.forward.androids.utils;

import android.graphics.Point;
import android.support.v4.widget.TextViewCompat;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author ziwei huang
 */
public class EllipsizeUtils {

    public static final int HIGHLIGHT_FIRST = 0;
    public static final int HIGHLIGHT_LAST = 1;
    public static final int HIGHLIGHT_ALL = 2;

    private static final String ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)

    /**
     * @param content
     * @param keyword
     * @param color
     * @param fromIndex     the index to begin highlighting from.
     * @param highlightType {@link #HIGHLIGHT_FIRST}, {@link #HIGHLIGHT_LAST}, {@link #HIGHLIGHT_ALL}
     * @param ignoreCase
     * @return the content highlighted, or origin content if {@code fromIndex >= content.length() }
     */
    public static SpannableString highlight(String content, String keyword, int color, int fromIndex, int highlightType, boolean ignoreCase) {
        SpannableString ss = new SpannableString(content);
        if (fromIndex >= content.length()
                || TextUtils.isEmpty(content) || TextUtils.isEmpty(keyword)) {
            return ss;
        }

        fromIndex = Math.max(0, fromIndex);
        String compareContent = ignoreCase ? content.toLowerCase(Locale.ENGLISH) : content;
        String compareKeyword = ignoreCase ? keyword.toLowerCase(Locale.ENGLISH) : keyword;

        if (highlightType == HIGHLIGHT_LAST ||
                highlightType == HIGHLIGHT_FIRST) {
            int index = -1;
            if (highlightType == HIGHLIGHT_LAST) {
                index = compareContent.lastIndexOf(compareKeyword);
                if (index < fromIndex) {
                    index = -1;
                }
            } else {
                index = compareContent.indexOf(compareKeyword, fromIndex);
            }
            if (index > -1) {
                ss.setSpan(new ForegroundColorSpan(color), index, index + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else { // HIGHLIGHT_ALL
            int index = compareContent.indexOf(compareKeyword, fromIndex);
            while (index >= 0) {
                ss.setSpan(new ForegroundColorSpan(color), index, index + keyword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = compareContent.indexOf(compareKeyword, index + keyword.length());
            }
        }

        return ss;
    }

    public static void ellipsizeByKeyword(final TextView textView, final String content, final String keyword, final boolean ignoreCase) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(keyword)) {
            textView.setText(null);
            return;
        }

        if (textView.getWidth() <= 0) {
            // Monitor layout completed
            new EllipseListener(textView, content, keyword, ignoreCase);
        } else {
            ellipiseInner(textView, content, keyword, ignoreCase);
        }
    }

    /**
     * @param textView
     * @param content
     * @param keyword
     * @param highlightColor
     * @param highlightAll   {@code true} if highlight all matched keyword. {@code true} if only highlight the first matched keyword from ellipised content
     * @param ignoreCase
     */
    public static void ellipsizeAndHighlight(final TextView textView, final String content, final String keyword, final int highlightColor, final boolean highlightAll, final boolean ignoreCase) {
        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(keyword)) {
            textView.setText(null);
            return;
        }

        if (textView.getWidth() <= 0) {
            // Monitor layout completed
            new EllipseListener(textView, content, keyword, highlightColor, highlightAll, ignoreCase);
        } else {
            ellipiseInner(textView, content, keyword, ignoreCase);

            int type = HIGHLIGHT_ALL;
            if (!highlightAll) {
                if (textView.getEllipsize() == TextUtils.TruncateAt.START) {
                    type = HIGHLIGHT_LAST;
                } else {
                    type = HIGHLIGHT_FIRST;
                }
            }
            SpannableString s = highlight(textView.getText().toString(), keyword, highlightColor, 0, type, ignoreCase);
            textView.setText(s);
        }
    }

    private static void ellipiseInner(final TextView textView, String content, String keyword, boolean ignoreCase) {
        TextPaint paint = textView.getPaint();
        if (paint == null) {
            textView.setText(null);
            return;
        }

        String compareContent = ignoreCase ? content.toLowerCase(Locale.ENGLISH) : content;
        String compareKeyword = ignoreCase ? keyword.toLowerCase(Locale.ENGLISH) : keyword;

        final int keywordStart = compareContent.indexOf(compareKeyword);
        if (keywordStart < 0) {
            textView.setText(null);
            return;
        }

        int maxLine = TextViewCompat.getMaxLines(textView);
        if (maxLine <= 0) {
            textView.setText(content);
            return;
        }

        int availableWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
        if (maxLine < 2) { // single line
            int availableCount = 0;
            // ellipse end
            String newCharSeq = TextUtils.ellipsize(compareContent, paint, availableWidth, TextUtils.TruncateAt.END).toString();
            availableCount = newCharSeq.length();
            if (newCharSeq.contains(compareKeyword)) {
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setText(content);
                return;
            }
            // ellipse start
            newCharSeq = TextUtils.ellipsize(compareContent, paint, availableWidth, TextUtils.TruncateAt.START).toString();
            availableCount = Math.max(newCharSeq.length(), availableCount);
            if (newCharSeq.contains(compareKeyword)) {
                textView.setEllipsize(TextUtils.TruncateAt.START);
                textView.setText(content);
                return;
            }

            // keyword is too long
            if (availableCount <= keyword.length()) {
                // keyword is too long. display: ELLIPSIS_NORMAL + keyword + ...
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setText(ELLIPSIS_NORMAL + keyword);
                return;
            }

            // display: ELLIPSIS_NORMAL + xxx + keyword + xxx +...
            textView.setEllipsize(TextUtils.TruncateAt.END);
            int start = keywordStart - (availableCount - keyword.length()) / 2;
            textView.setText(ELLIPSIS_NORMAL + content.substring(start >= 0 ? start : 0));
        } else { // multi line
            List<Point> linesStart = getLineStartAndEnd(textView.getPaint(), compareContent, availableWidth);
            int keywordLineStart = getKeywordLine(keywordStart, linesStart);
            int keywordLineEnd = getKeywordLine(keywordStart + compareKeyword.length() + 1, linesStart);
            if (keywordLineEnd - keywordLineStart < maxLine) {
                int endLine = Math.min(keywordLineStart + maxLine / 2, linesStart.size() - 1); // linesStart.size() - 1 = lastLines
                int startLine = Math.max(endLine - (maxLine - 1) + maxLine % 2, 0); // if maxline is odd, starline+1
                // display: ELLIPSIS_NORMAL + xxx + keyword + xxx +...
                textView.setEllipsize(TextUtils.TruncateAt.END);
                int start = linesStart.get(startLine).x;
                if (startLine == 0) {
                    textView.setText(content.substring(start >= 0 ? start : 0));
                } else {
                    textView.setText(ELLIPSIS_NORMAL + content.substring(start >= 0 ? start : 0));
                }
            } else { // keyword is too long
                // keyword is too long. display: ELLIPSIS_NORMAL + keyword + xx ...
                textView.setEllipsize(TextUtils.TruncateAt.END);
                textView.setText(ELLIPSIS_NORMAL + content.substring(keywordStart));
            }
        }
    }

    private static int getKeywordLine(int keywordStart, List<Point> linesStart) {
        for (int i = 0; i < linesStart.size(); i++) {
            if (keywordStart < linesStart.get(i).y) {
                return i;
            }
        }
        return 0;
    }

    /**
     * @return point.x is start position, point.y is end position
     */
    private static List<Point> getLineStartAndEnd(TextPaint tp, CharSequence cs, int lineWidth) {
        StaticLayout layout = new StaticLayout(cs, tp, lineWidth, Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, true);
        int count = layout.getLineCount();

        List<Point> list = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            list.add(new Point(layout.getLineStart(i), layout.getLineEnd(i)));
        }
        return list;
    }

    private static class EllipseListener implements ViewTreeObserver.OnPreDrawListener {
        final TextView textView;
        final String content;
        final String keyword;
        final int highlightColor;
        final boolean highlightAll;
        final boolean ignoreCase;
        boolean needHighlight;

        public EllipseListener(TextView tv, String content, String keyword, boolean ignoreCase) {
            this(tv, content, keyword, 0, false, ignoreCase);
            this.needHighlight = false;
        }

        public EllipseListener(TextView tv, String content, String keyword, int highlightColor, boolean highlightAll, boolean ignoreCase) {
            this.textView = tv;
            this.content = content;
            this.keyword = keyword;
            this.highlightColor = highlightColor;
            this.highlightAll = highlightAll;
            this.ignoreCase = ignoreCase;
            this.needHighlight = true;

            tv.getViewTreeObserver().addOnPreDrawListener(this);
        }

        @Override
        public boolean onPreDraw() {
            textView.getViewTreeObserver().removeOnPreDrawListener(this);

            ellipiseInner(textView, content, keyword, ignoreCase);
            if (!needHighlight) {
                return true;
            }

            int type = HIGHLIGHT_ALL;
            if (!highlightAll) {
                if (textView.getEllipsize() == TextUtils.TruncateAt.START) {
                    type = HIGHLIGHT_LAST;
                } else {
                    type = HIGHLIGHT_FIRST;
                }
            }
            SpannableString s = highlight(textView.getText().toString(), keyword, highlightColor, 0, type, ignoreCase);
            textView.setText(s);
            return true;
        }
    }

    public static void ellipsize(TextView textView, String content) {
        TextUtils.TruncateAt ellipsize = textView.getEllipsize();
        if (ellipsize != TextUtils.TruncateAt.START && ellipsize != TextUtils.TruncateAt.MIDDLE) { // handle it over to the system
            textView.setText(content);
            return;
        }

        int maxLine = TextViewCompat.getMaxLines(textView);
        int availableWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
        if (maxLine < 2) { // single line
            textView.setText(content);
        } else {
            List<Point> linesStart = getLineStartAndEnd(textView.getPaint(), content, availableWidth);
            if (linesStart.size() <= maxLine) {
                textView.setText(content);
                return;
            }

            if (ellipsize == TextUtils.TruncateAt.START) {
                int start = linesStart.get(linesStart.size() - maxLine).x;
                start = Math.max(start + ELLIPSIS_NORMAL.length(), 0);
                String substring = content.substring(start);
                while (getLayout(textView.getPaint(), ELLIPSIS_NORMAL + substring, availableWidth).getLineCount() > maxLine) {
                    int firstSpace = substring.indexOf(' '); // break
                    if (firstSpace == -1) {
                        substring = substring.substring(1);
                    } else {
                        substring = substring.substring(firstSpace + 1);
                    }
                }
                textView.setText(ELLIPSIS_NORMAL + substring);
            } else { // middle
                int middleLineStart = (maxLine - 1) / 2;
                Point point = linesStart.get(middleLineStart);
                int startEllipsize = point.y - ELLIPSIS_NORMAL.length();
                final String substringStart = content.substring(0, startEllipsize);

                int middleLineEnd = linesStart.size() - (maxLine - (maxLine - 1) / 2 - 1);
                Point pointEnd = linesStart.get(middleLineEnd);
                String substringEnd = content.substring(pointEnd.x);
                while (getLayout(textView.getPaint(), substringStart + ELLIPSIS_NORMAL + substringEnd, availableWidth).getLineCount() > maxLine) {
                    int firstSpace = substringEnd.indexOf(' '); // break
                    if (firstSpace == -1) {
                        substringEnd = substringEnd.substring(1);
                    } else {
                        substringEnd = substringEnd.substring(firstSpace + 1);
                    }
                }
                textView.setText(substringStart + ELLIPSIS_NORMAL + substringEnd);
            }

        }
    }

    private static Layout getLayout(TextPaint tp, CharSequence cs, int lineWidth) {
        StaticLayout layout = new StaticLayout(cs, tp, lineWidth, Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, true);
        return layout;
    }

}
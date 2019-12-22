package com.fastchar.system;

import com.fastchar.core.FastChar;

import java.io.*;
import java.nio.charset.Charset;

public class FastOutPrintStream extends PrintStream {
    static final Object PrintLock = new Object();

    /**
     * Creates a new print stream.  This stream will not flush automatically.
     *
     * @param out The output stream to which values and objects will be
     *            printed
     * @see PrintWriter#PrintWriter(OutputStream)
     */
    public FastOutPrintStream(OutputStream out) {
        super(out);
    }

    /**
     * Creates a new print stream.
     *
     * @param out       The output stream to which values and objects will be
     *                  printed
     * @param autoFlush A boolean; if true, the output buffer will be flushed
     *                  whenever a byte array is written, one of the
     *                  <code>println</code> methods is invoked, or a newline
     *                  character or byte (<code>'\n'</code>) is written
     * @see PrintWriter#PrintWriter(OutputStream, boolean)
     */
    public FastOutPrintStream(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    /**
     * Creates a new print stream.
     *
     * @param out       The output stream to which values and objects will be
     *                  printed
     * @param autoFlush A boolean; if true, the output buffer will be flushed
     *                  whenever a byte array is written, one of the
     *                  <code>println</code> methods is invoked, or a newline
     *                  character or byte (<code>'\n'</code>) is written
     * @param encoding  The name of a supported
     *                  <a href="../lang/package-summary.html#charenc">
     *                  character encoding</a>
     * @throws UnsupportedEncodingException If the named encoding is not supported
     * @since 1.4
     */
    public FastOutPrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException {
        super(out, autoFlush, encoding);
    }

    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file name.  This convenience constructor creates
     * the necessary intermediate {@link OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the
     * {@linkplain Charset#defaultCharset() default charset}
     * for this newInstance of the Java virtual machine.
     *
     * @param fileName The name of the file to use as the destination of this print
     *                 stream.  If the file exists, then it will be truncated to
     *                 zero size; otherwise, a new file will be created.  The output
     *                 will be written to the file and is buffered.
     * @throws FileNotFoundException If the given file object does not denote an existing, writable
     *                               regular file and a new regular file of that name cannot be
     *                               created, or if some other error occurs while opening or
     *                               creating the file
     * @throws SecurityException     If a security manager is present and {@link
     *                               SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                               access to the file
     * @since 1.5
     */
    public FastOutPrintStream(String fileName) throws FileNotFoundException {
        super(fileName);
    }

    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file name and charset.  This convenience constructor creates
     * the necessary intermediate {@link OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param fileName The name of the file to use as the destination of this print
     *                 stream.  If the file exists, then it will be truncated to
     *                 zero size; otherwise, a new file will be created.  The output
     *                 will be written to the file and is buffered.
     * @param csn      The name of a supported {@linkplain Charset
     *                 charset}
     * @throws FileNotFoundException        If the given file object does not denote an existing, writable
     *                                      regular file and a new regular file of that name cannot be
     *                                      created, or if some other error occurs while opening or
     *                                      creating the file
     * @throws SecurityException            If a security manager is present and {@link
     *                                      SecurityManager#checkWrite checkWrite(fileName)} denies write
     *                                      access to the file
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since 1.5
     */
    public FastOutPrintStream(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(fileName, csn);
    }

    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file.  This convenience constructor creates the necessary
     * intermediate {@link OutputStreamWriter OutputStreamWriter},
     * which will encode characters using the {@linkplain
     * Charset#defaultCharset() default charset} for this
     * newInstance of the Java virtual machine.
     *
     * @param file The file to use as the destination of this print stream.  If the
     *             file exists, then it will be truncated to zero size; otherwise,
     *             a new file will be created.  The output will be written to the
     *             file and is buffered.
     * @throws FileNotFoundException If the given file object does not denote an existing, writable
     *                               regular file and a new regular file of that name cannot be
     *                               created, or if some other error occurs while opening or
     *                               creating the file
     * @throws SecurityException     If a security manager is present and {@link
     *                               SecurityManager#checkWrite checkWrite(file.getPath())}
     *                               denies write access to the file
     * @since 1.5
     */
    public FastOutPrintStream(File file) throws FileNotFoundException {
        super(file);
    }

    /**
     * Creates a new print stream, without automatic line flushing, with the
     * specified file and charset.  This convenience constructor creates
     * the necessary intermediate {@link OutputStreamWriter
     * OutputStreamWriter}, which will encode characters using the provided
     * charset.
     *
     * @param file The file to use as the destination of this print stream.  If the
     *             file exists, then it will be truncated to zero size; otherwise,
     *             a new file will be created.  The output will be written to the
     *             file and is buffered.
     * @param csn  The name of a supported {@linkplain Charset
     *             charset}
     * @throws FileNotFoundException        If the given file object does not denote an existing, writable
     *                                      regular file and a new regular file of that name cannot be
     *                                      created, or if some other error occurs while opening or
     *                                      creating the file
     * @throws SecurityException            If a security manager is present and {@link
     *                                      SecurityManager#checkWrite checkWrite(file.getPath())}
     *                                      denies write access to the file
     * @throws UnsupportedEncodingException If the named charset is not supported
     * @since 1.5
     */
    public FastOutPrintStream(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(file, csn);
    }

    @Override
    public void println(boolean x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(char x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(int x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(long x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(float x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(double x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(char[] x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(String x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println(Object x) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println(x);
    }

    @Override
    public void println() {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        super.println();
    }

    @Override
    public void print(String s) {
        if (!FastChar.getConstant().isSystemOutPrint()) {
            return;
        }
        synchronized (PrintLock) {
            super.print(s);
        }
    }

}

package edu.kaist.salab.jriext.instrumentation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Created by byron1st on 2016. 1. 14..
 */
public class JRIEXTMethodVisitorDebugMode extends AdviceAdapter implements Opcodes{
    private MonitoringUnit monitoringUnit;

    public JRIEXTMethodVisitorDebugMode(MethodVisitor mv, int access, String name, String desc, MonitoringUnit monitoringUnit) {
        super(ASM5, mv, access, name, desc);
        this.monitoringUnit = monitoringUnit;
    }

    @Override
    protected void onMethodEnter() {
        if(isFeasible(methodAccess) && monitoringUnit.isEnter()) insertLoggingCode();
    }

    @Override
    protected void onMethodExit(int opcode) {
        if(isFeasible(methodAccess) && !monitoringUnit.isEnter()) insertLoggingCode();
        mv.visitEnd();
    }

    private void insertLoggingCode() {
        Label ifSystemOutNull = new Label();
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitJumpInsn(IFNULL, ifSystemOutNull);
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getStackTrace", "()[Ljava/lang/StackTraceElement;", false);
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "toString", "([Ljava/lang/Object;)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        mv.visitLabel(ifSystemOutNull);
    }

    private boolean isFeasible(int access) {
        return !((access & ACC_NATIVE) != 0)
                || ((access & ACC_ABSTRACT) != 0);
    }
}

package edu.kaist.salab.jriext.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Created by byron1st on 2016. 1. 8..
 */
public class JRIEXTClassVisitor extends ClassVisitor implements Opcodes{
    private MonitoringUnit monitoringUnit;
    private boolean isDebugMode;

    public JRIEXTClassVisitor(ClassVisitor cv, MonitoringUnit monitoringUnit, boolean isDebugMode) {
        super(ASM5, cv);
        this.monitoringUnit = monitoringUnit;
        this.isDebugMode = isDebugMode;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if(monitoringUnit.getMethodName().equals(name) && monitoringUnit.getMethodDesc().equals(desc)) {
            if(isDebugMode)
                return new JRIEXTMethodVisitorDebugMode(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, monitoringUnit);
            return new JRIEXTMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, monitoringUnit);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }
}

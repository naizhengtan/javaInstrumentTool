package memop;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IADD;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.InstructionFinder;

public class MemInstr {
	
	String[] instrumentOps = {"GETFIELD","PUTFIELD",
			"AASTORE","ASTORE","BASTORE","CASTORE","DASTORE",
			"DSTORE","FASTORE","FSTORE","IASTORE","ISTORE","LASTORE","LSTORE","SASTORE",
			"AALOAD","ALOAD","BALOAD","CALOAD","DALOAD",
			"DLOAD","FALOAD","FLOAD","IALOAD","ILOAD","LALOAD","LLOAD","SALOAD"
			};
	
	private String generatePattern(){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<instrumentOps.length-1;i++){
			sb.append(instrumentOps[i]);
			sb.append("|");
		}
		sb.append(instrumentOps[instrumentOps.length-1]);
		return sb.toString();
	}
	
	private void printFields(ClassGen classGen){
		ConstantPoolGen cPoolGen = classGen.getConstantPool(); 
		Field[] tmpfields = classGen.getFields();
		for(int i=0;i<tmpfields.length;i++){
			System.out.println(tmpfields[i]);
		}

		for(int i=0;i<cPoolGen.getSize();i++){
			System.out.println(cPoolGen.getConstant(i));
		}
	}
	
	private void printMethod(MethodGen methodGen){
		InstructionList list = methodGen.getInstructionList();
		InstructionHandle[] ih = list.getInstructionHandles();
		for(int j=0;j<ih.length;j++){
			System.out.println(ih[j]);
		}
	}
	
	private void dumpToFile(JavaClass target){
		try {
			target.dump("/Users/naizheng/tmp/memop/test.class");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void instrumentOp(InstructionList list, InstructionHandle curop, int lv){
		curop = list.append(curop,new ALOAD(0)); // this
		curop = list.append(curop, new DUP());  // this, this
		curop = list.append(curop, new GETFIELD(lv)); //this,rcounter
		curop = list.append(curop, new ICONST(1)); //this,rcounter,1
		curop = list.append(curop, new IADD()); //this, rcounter+1
		curop = list.append(curop, new PUTFIELD(lv)); //...
	}
	
	private void instrumentOp2(InstructionList list, InstructionHandle curop, int lv){
		curop = list.append(curop, new GETSTATIC(lv)); //this,rcounter
		curop = list.append(curop, new ICONST(1)); //this,rcounter,1
		curop = list.append(curop, new IADD()); //this, rcounter+1
		curop = list.append(curop, new PUTSTATIC(lv)); //...
	}


	private byte[] InstrumentMemOp(JavaClass clazz, String imethod){
		//initialization
		ClassGen classGen = new ClassGen(clazz);
		ConstantPoolGen cPoolGen = classGen.getConstantPool(); 

		//add the counter
		FieldGen rc = new FieldGen(Constants.ACC_PUBLIC|Constants.ACC_STATIC, Type.INT,"memop_read_counter",cPoolGen);
		FieldGen wc = new FieldGen(Constants.ACC_PUBLIC|Constants.ACC_STATIC, Type.INT,"memop_write_counter",cPoolGen);
		classGen.addField(rc.getField());
		classGen.addField(wc.getField());
		int rindex = cPoolGen.addFieldref(classGen.getClassName(), "memop_read_counter", "I");
		int windex = cPoolGen.addFieldref(classGen.getClassName(), "memop_write_counter", "I");

		//instrument the methods
		Method[] methods = clazz.getMethods();
		//instrument pattern
		String pattern = generatePattern();
		System.out.println("Instrumented operations: \n  "+pattern);
		
		for(int i=0;i<methods.length;i++){
			//method info init
			MethodGen methodGen = new MethodGen(methods[i], clazz.getClassName(), cPoolGen); 
			InstructionList list = methodGen.getInstructionList();
			//System.out.println("local: "+methodGen.getMaxLocals()+" stack: "+methodGen.getMaxStack());
			
			if(imethod!=null && !methodGen.getName().equals(imethod))
				continue;
			
			//search for the read op
			InstructionFinder insf = new InstructionFinder(list);
			for(Iterator it = insf.search(pattern); it.hasNext(); ) {
				InstructionHandle[] ins = (InstructionHandle[]) it.next();
				if(ins[0].getInstruction().getName().equals("getfield")){
					instrumentOp2(list,ins[0],rindex);
					//System.out.println("R: "+ins[0]);
				}else if(ins[0].getInstruction().getName().equals("putfield")){
					instrumentOp2(list,ins[0],windex);
					//System.out.println("W "+ins[0]);
				}else if(ins[0].getInstruction().getName().contains("load")){
					instrumentOp2(list,ins[0],rindex);
					//System.out.println("R "+ins[0]);
				}else if(ins[0].getInstruction().getName().contains("store")){
					instrumentOp2(list,ins[0],windex);
					//System.out.println("W "+ins[0]);
				}else{
					System.err.println("Error, unrecognized op");
				}
			}
			
			//reset the stack size
			methodGen.setMaxStack(methodGen.getMaxStack()+2);
			//replace the method
			classGen.replaceMethod(methods[i], methodGen.getMethod());
			//using low version, bypass stackMap
			classGen.setMajor(50);
			
			//System.out.println(methodGen.getName());
			//printMethod(methodGen);
		}

		//generate result
		JavaClass target = classGen.getJavaClass(); 
		//dumpToFile(target);
		return target.getBytes();
	}

	public static byte[] instrumentClass(Class clazz, String imethod){
		MemInstr instr = new MemInstr();
		JavaClass jc = null;
		try {
			jc = Repository.lookupClass(clazz);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return instr.InstrumentMemOp(jc,imethod);
	}
	
	public static byte[] instrumentClass(Class clazz){
		return instrumentClass(clazz,null);
	}
	
	public static void main(String[] args){
		instrumentClass(Test.class,"test");
	}

}

package memop;

import java.lang.reflect.Field;

public class InstrumentClassLoader extends ClassLoader{

	//FIXME: this should not be hard-coded
	public String instrumentClass = "memop.Test";
	public String instrumentMethod = "test";
	Class instrumentClazz;

	public InstrumentClassLoader() {
		super();
		// TODO Auto-generated constructor stub
	}

	public InstrumentClassLoader(ClassLoader parent) {
		super(parent);
		// TODO Auto-generated constructor stub
	}

	private void endOfProgram() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				//System.out.println("This is the end of the program.");
				printResult(instrumentClazz);
			}
		});
	}

	private void printResult(Class clazz){

		if(clazz==null)
			return;
		try {
			Field rc = clazz.getDeclaredField("memop_read_counter");
			Field wc = clazz.getDeclaredField("memop_write_counter");
			System.out.println("read operations: "+rc.getInt(rc));
			System.out.println("write operations: "+wc.getInt(wc));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		// TODO Auto-generated method stub
		Class clazz =  super.loadClass(name);
		byte[] clazzStream = null;

		if(name.equals(instrumentClass)){
			//clazzStream = MemInstr.instrumentClass(clazz,instrumentMethod);
			clazzStream = MemInstr.instrumentClass(clazz);
			clazz = super.defineClass(name, clazzStream, 0, clazzStream.length);
			//printResult(clazz);
			instrumentClazz = clazz;
			endOfProgram();
		}
		//System.out.println("return clazz "+clazz);

		return clazz;
	}


}

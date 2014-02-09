package memop;

public class Test {
	int blablabla;
	int hhhh;


	int[] test(int[] b, int[] c) {
		int[] a = new int[10];
		for (int i=0; i<10; i++) {
			a[i] =b[i] + c[i];
		}
		return a;
	}

	void hoho(){
		blablabla++;
		System.out.println(" the number is "+blablabla);
	}

	public static void main(String args[]){
		Test a = new Test();
		int[] b = new int[10];
		int[] c = new int[10];
		for(int i=0;i<10;i++){
			b[i] = i;
			c[i] = i*2;
		}
		a.test(b,c);
	}
}

package ncl.cs.prime.opencl;

public class GenRunAlphaCL {

	public static final String[] MODE_NAMES = {"sqrt" , "int", "log"}; // , "float"};
	public static int[][] CORE_SETUP = {
			{1},
			{1, 2, 4, 8, 16, 32, 64, 128, 256, 512},
			{1, 4, 16, 64, 256, 1024}
		};
	public static int[][] WL_SETUP = {
			{80000000, 4000000, 80000000},
			{80000000, 8000000, 80000000},
			{80000000, 4000000, 1000000}
		};
	
	public static void main(String[] args) {
		System.out.printf("REM Alpha determination\nREM b m w j p z n0 n1 n2\n@ECHO OFF\n");
		int count = 0;
		for(int m=0; m<MODE_NAMES.length; m++) {
			System.out.printf("\nREM %s\n", MODE_NAMES[m]);
			for(int z=0; z<3; z++) {
				int[] n = {0, 0, 0};
				for(int d=0; d<CORE_SETUP[z].length; d++) {
					n[z] = CORE_SETUP[z][d];
					System.out.printf("CALL run.bat eq %d %d 1.0 1.0 %d %d %d %d\n", m, WL_SETUP[m][z],
							z, n[0], n[1], n[2]);
					count++;
				}
			}
		}
		System.out.printf("\nREM num of experiments: %d\n", count);
	}
}

import java.util.ArrayList;

/**
 * Runnable that searches for a subset of 'array' that sums to 'b'.
 * Two threads run this concurrently, the first to find a solution sets the shared flag.
 */
public class ThreadCheckArray implements Runnable
{
	private boolean flag;
	private boolean[] winArray; // marks which elements are part of the solution
	SharedData sd;
	ArrayList<Integer> array;
	int b;

	public ThreadCheckArray(SharedData sd)
	{
		this.sd = sd;
		synchronized (sd)
		{ 
			array = sd.getArray();
			b = sd.getB();
		}
		winArray = new boolean[array.size()];
	}

	/**
	 * Recursively checks if a subset of array[0..n-1] sums to b.
	 * Stops early if another thread already found a solution.
	 */
	void rec(int n, int b)
	{
		synchronized (sd)
		{
			if (sd.getFlag()) // another thread found a solution
				return;
		}
		if (n == 1)
		{
			if(b == 0 || b == array.get(n-1))
			{
				flag = true;
				synchronized (sd)
				{
					sd.setFlag(true);
				}
			}
			if (b == array.get(n-1))
				winArray[n-1] = true;
			return;
		}

		rec(n-1, b - array.get(n-1)); // include element n-1
		if (flag)
			winArray[n-1] = true;
		synchronized (sd)
		{
			if (sd.getFlag())
				return;
		}
		rec(n-1, b); // exclude element n-1
	}

	public void run() {
		// thread1 includes the last element; thread2 excludes it
		if (array.size() != 1)
			if (Thread.currentThread().getName().equals("thread1"))
				rec(array.size()-1, b - array.get(array.size() - 1));
			else
				rec(array.size()-1, b);
		if (array.size() == 1)
			if (b == array.get(0) && !flag)
			{
				winArray[0] = true;
				flag = true;
				synchronized (sd)
				{
					sd.setFlag(true);
				}
			}
		if (flag)
		{
			if (Thread.currentThread().getName().equals("thread1"))
				winArray[array.size() - 1] = true;
			synchronized (sd)
			{
				sd.setWinArray(winArray); // save result to shared data
			}
		}
	}
}
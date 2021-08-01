package mitm.atb;

public class Interval {

	public int lower;
	public int upper;
	public int length;

	public Interval(int lower, int upper) {
		this.lower = lower;
		this.upper = upper;
		this.length = upper - lower;
	}

	public boolean contains(int x) {
		if (x >= lower && x < upper)
			return true;
		else
			return false;
	}

	public boolean starts(Interval other) {
		if (other.lower == lower)
			return true;
		else
			return false;
	}

	public boolean finishes(Interval other) {
		if (other.upper == upper)
			return true;
		else
			return false;
	}

	public boolean preceedsGenerally(Interval other) {
		if (upper <= other.lower)
			return true;
		else
			return false;
	}

	public boolean preceedsImmediately(Interval other) {
		if (upper == other.lower)
			return true;
		else
			return false;
	}

	public boolean succeedsGenerally(Interval other) {
		if (lower > other.upper)
			return true;
		else
			return false;
	}

	public boolean succeedsImmediately(Interval other) {
		if (lower == other.upper) {
			return true;
		} else
			return false;
	}

	public boolean overlaps(Interval other) {
		if (other.lower < upper && lower < other.upper) {
			return true;
		} else
			return false;
	}

	public boolean during(Interval other) {
		if ((lower > other.lower && upper <= other.upper) || (lower >= other.lower && upper < other.upper)) {
			return true;
		} else
			return false;
	}

	public Interval translate(int x) {
		if (x < 0)
			return null;

		return new Interval(lower + x, upper + x);
	}

	@Override
	public String toString() {
		return "s[" + lower + "," + upper + ")";
	}
}

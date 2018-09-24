package org.orbital.orbit;

import org.orbital.math.kinematics.GravitationalForce;
import org.orbital.math.kinematics.Mass;
import org.orbital.math.kinematics.Particle;
import org.orbital.math.vector.CartesianVector;
import org.orbital.math.vector.LagrangianVector;

public class Orbit {
	public static final double KEPLER_TOLERANCE = Math.pow(10, -8);
	
	public static final int KEPLER_MAX_ITERATIONS = 1000;
	
	private final static double RADIANS_TO_DEGREES = 180.0 / Math.PI;
	
	public static void main(String[] args) {
		Mass defaultMass = new Mass("m", 5.972e24);
		Mass defaultMass2 = new Mass("m2", 1000);
		
		CartesianVector r1 = new CartesianVector("R_1", 0, 0, 0);
		CartesianVector v1 = new CartesianVector("V_1", 0, 0, 0);
		CartesianVector a1 = new CartesianVector("A_1", 0, 0, 0);
		
		Particle body = new Particle("Earth", defaultMass, r1, v1, a1, 6378);
		
		CartesianVector r2 = new CartesianVector("R_2", 1600, 5310, 3800);
		CartesianVector v2 = new CartesianVector("V_2", -7.350, 0.4600, 2.470);
		CartesianVector a2 = new CartesianVector("A_2", 0, 0, 0);
		Particle satellite = new Particle("Satellite", defaultMass2, r2, v2, a2);
		
		satellite.addForce(new GravitationalForce(body, satellite));
	
		Orbit o = new Orbit(body, satellite);
		
		//System.out.println(satellite.getR());
		
		//System.out.println(satellite.getV());
		
		//System.out.println(satellite.getA());
		
		//System.out.println(o);
		
		//System.out.println(cua());
		
		System.out.println(o);
		
		System.out.println(o.calculateUniversalAnomaly(3600.0));
		
		System.out.println(o.calculateLagrangian(3600.0));
		
		long startTime = System.currentTimeMillis();
		
		for (int seconds = 0; seconds < o.calculatePeriod(); seconds++) {
			Particle satellitePrime = o.satellite.scaleLagrangian(o.calculateLagrangian(seconds));
			
			System.out.println(seconds + "s\t" + satellitePrime.r.toSmallString() + "\t" + 
			   satellitePrime.v.getMagnitude() + "km/s (" + satellitePrime.v.toSmallString() + ")");
		}
		
		System.out.println("Execution took " + (System.currentTimeMillis() - startTime) + "ms for " + o.calculatePeriod() + " seconds of simulation.");
	}
	
	protected final Particle body;
	
	protected final Particle satellite;

	public Orbit(Particle body, Particle satellite) {
		super();
		this.body = body;
		this.satellite = satellite;
	}
	
	public CartesianVector calculateAngularVector() {
		return this.satellite.r.crossProduct(this.satellite.v);
	}
	
	public CartesianVector calculateNodeLine() {
		CartesianVector k = new CartesianVector("K", 0.0, 0.0, 1.0);
		return k.crossProduct(this.calculateAngularVector());
	}
	
	public double calculateRightAscension() {
		CartesianVector n = calculateNodeLine();
		if (n.y.getMagnitude() >= 0)
			return Math.acos(n.x.getMagnitude() / n.getMagnitude()) * RADIANS_TO_DEGREES;
		else
			return 360 - Math.acos(n.x.getMagnitude() / n.getMagnitude()) * RADIANS_TO_DEGREES;
	}
	
	public double calculatePerigeeArgument() {
		CartesianVector n = this.calculateNodeLine();
		CartesianVector e = this.calculateEccentricityVector();
		if (e.z.getMagnitude() >= 0)
			return Math.acos(n.scale("1/n", 1.0 / n.getMagnitude()).dotProduct(
				e.scale("1/e", 1.0 / e.getMagnitude()))) * RADIANS_TO_DEGREES;
		else 
			return 360.0 - Math.acos(n.scale("1/n", 1.0 / n.getMagnitude()).dotProduct(
					e.scale("1/e", 1.0 / e.getMagnitude()))) * RADIANS_TO_DEGREES;
	}
	
	public double calculateTrueAnomaly() {
		CartesianVector e = this.calculateEccentricityVector();
		CartesianVector r = this.satellite.r;
		if (this.calculateRadialVelocity() >= 0)
			return Math.acos(e.scale("1/e", 1.0 / e.getMagnitude()).dotProduct(
				r.scale("1/r", 1.0 / r.getMagnitude()))) * RADIANS_TO_DEGREES;
		else 
			return 360 - Math.acos(e.scale("1/e", 1.0 / e.getMagnitude()).dotProduct(
					r.scale("1/r", 1.0 / r.getMagnitude()))) * RADIANS_TO_DEGREES;
	}
	
	public CartesianVector calculateEccentricityVector() {
		double r = this.satellite.r.getMagnitude();
		double u = this.body.gravitationalParameter;
		CartesianVector rPrime = this.satellite.r.scale("u/r", u / r);
		CartesianVector vPrime = this.satellite.v.crossProduct(this.calculateAngularVector());
		return vPrime.subtract(rPrime).scale("1/u", 1.0 / u);
	}
	
	public double calculateEccentricity() {
		return this.calculateEccentricityVector().getMagnitude();
	}
	
	public double calculateRadialVelocity() {
		double r = this.satellite.r.getMagnitude();
		return this.satellite.v.dotProduct(this.satellite.r) / r;
	}
	
	public double calculateInclination() {
		CartesianVector h = calculateAngularVector();
		return Math.acos(h.z.getMagnitude() / h.getMagnitude()) * RADIANS_TO_DEGREES;
	}
	
	public double calculatePerigee() {
		double h = this.calculateAngularVector().getMagnitude();
		double u = this.body.gravitationalParameter;
		double e = this.calculateEccentricity();
		return Math.pow(h, 2.0) / u * (1 / (1 + e * Math.cos(0)));
	}
	
	public double calculateApogee() {
		double h = this.calculateAngularVector().getMagnitude();
		double u = this.body.gravitationalParameter;
		double e = this.calculateEccentricity();
		return Math.pow(h, 2.0) / u * (1 / (1 + e * Math.cos(Math.PI)));
	}
	
	public double calculateSemimajorAxis() {
		return 0.5 * (this.calculatePerigee() + this.calculateApogee());
	}
	
	public double calculatePeriod() {
		return (2.0 * Math.PI * Math.pow(this.calculateSemimajorAxis(), 1.5)) / Math.sqrt(this.body.gravitationalParameter);
	}
	
	public static double cua() {
		double deltaTime = 3600;
		double u = 398600;
		double a = 1.0 / -19655;
		double ua = Math.sqrt(u) * Math.abs(a) * deltaTime;
		double vr0 = 3.0752;
		double r0 = 10000;
		double z = -40.0;
		double sz, cz, f = 1.0, fPrime = 1.0, i = 0;
		while (Math.abs(f / fPrime) > Orbit.KEPLER_TOLERANCE && ++i < Orbit.KEPLER_MAX_ITERATIONS) {
			if (z > 0) {
				cz = (1.0 - Math.cos(Math.sqrt(z))) / z;
				sz = (Math.sqrt(z) - Math.sin(Math.sqrt(z))) / Math.pow(Math.sqrt(z), 3.0);
			} else if (z < 0) {
				cz = (Math.cosh(Math.sqrt(-z)) - 1.0) / -z;
				sz = (Math.sinh(Math.sqrt(-z)) - Math.sqrt(-z)) / Math.pow(Math.sqrt(-z), 3.0);
			} else {
				cz = 1.0 / 2.0;
				sz = 1.0 / 6.0;
			}
			f = ((r0 * vr0) / Math.sqrt(u)) * Math.pow(ua, 2.0) * cz 
					+ (1.0 - a * r0) * Math.pow(ua, 3.0) * sz 
					+ r0 * ua - Math.sqrt(u) * deltaTime;
			fPrime = ((r0 * vr0) / Math.sqrt(u)) * ua 
					* (1.0 - a * Math.pow(ua, 2.0) * sz)
					+ (1.0 - a * r0) * Math.pow(ua, 2.0) * cz
					+ r0;
			
			System.out.println("ua=" + ua + "\tz=" + z + "\tC(z)=" + cz + "\tS(z)=" + sz);
			
			ua = ua - f / fPrime;
			
			z = a * Math.pow(ua, 2.0);

			System.out.println(f + "=f\t" + fPrime + "=f'\t" + f / fPrime + "=ratio");
			System.out.println((Math.abs(f / fPrime) > Orbit.KEPLER_TOLERANCE) + "\t" + (i < Orbit.KEPLER_MAX_ITERATIONS));
		}
		return ua;
	}
	
	public static double calculateStumpffC(double z) {
		if (z > 0) {
			return (1.0 - Math.cos(Math.sqrt(z))) / z;
		} else if (z < 0) {
			return (Math.cosh(Math.sqrt(-z)) - 1.0) / -z;
		} else {
			return 1.0 / 2.0;
		}
	}
	
	public static double calculateStumpffS(double z) {
		if (z > 0) {
			return (Math.sqrt(z) - Math.sin(Math.sqrt(z))) / Math.pow(Math.sqrt(z), 3.0);
		} else if (z < 0) {
			return (Math.sinh(Math.sqrt(-z)) - Math.sqrt(-z)) / Math.pow(Math.sqrt(-z), 3.0);
		} else {
			return 1.0 / 6.0;
		}
	}
	
	public LagrangianVector calculateLagrangian(double deltaTime) {
		return new LagrangianVector(this, deltaTime);
	}
	
	public double calculateUniversalAnomaly(double deltaTime) {
		double u = this.body.gravitationalParameter;
		double a = 1.0 / this.calculateSemimajorAxis();
		double ua = Math.sqrt(u) * Math.abs(a) * deltaTime;
		double vr0 = this.calculateRadialVelocity();
		double r0 = this.satellite.r.getMagnitude();
		double z = a * Math.pow(ua, 2.0);
		double sz, cz, f = 1.0, fPrime = 1.0, i = 0;
		while (Math.abs(f / fPrime) > Orbit.KEPLER_TOLERANCE && ++i < Orbit.KEPLER_MAX_ITERATIONS) {
			cz = calculateStumpffC(z);
			sz = calculateStumpffS(z);
			f = ((r0 * vr0) / Math.sqrt(u)) * Math.pow(ua, 2.0) * cz 
					+ (1.0 - a * r0) * Math.pow(ua, 3.0) * sz 
					+ r0 * ua - Math.sqrt(u) * deltaTime;
			fPrime = ((r0 * vr0) / Math.sqrt(u)) * ua 
					* (1.0 - a * Math.pow(ua, 2.0) * sz)
					+ (1.0 - a * r0) * Math.pow(ua, 2.0) * cz
					+ r0;
			
			//System.out.println("ua=" + ua + "\tz=" + z + "\tC(z)=" + cz + "\tS(z)=" + sz);
			
			ua = ua - f / fPrime;
			
			z = a * Math.pow(ua, 2.0);

			//System.out.println(f + "=f\t" + fPrime + "=f'\t" + f / fPrime + "=ratio");
			//System.out.println((Math.abs(f / fPrime) > Orbit.KEPLER_TOLERANCE) + "\t" + (i < Orbit.KEPLER_MAX_ITERATIONS));
		}
		return ua;
	}
	
	public double calculateDistance() {
		return body.distance(satellite);
	}

	/**
	 * @return the body
	 */
	public Particle getBody() {
		return body;
	}

	/**
	 * @return the satellite
	 */
	public Particle getSatellite() {
		return satellite;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Orbit [calculateAngularVector()=" + calculateAngularVector()
				+ "\ncalculateNodeLine()=" + calculateNodeLine()
				+ "\ncalculateRightAscension()=" + calculateRightAscension()
				+ "\ncalculatePerigeeArgument()=" + calculatePerigeeArgument()
				+ "\ncalculateTrueAnomaly()=" + calculateTrueAnomaly()
				+ "\ncalculateEccentricityVector()="
				+ calculateEccentricityVector() + "\ncalculateEccentricity()="
				+ calculateEccentricity() + "\ncalculateRadialVelocity()="
				+ calculateRadialVelocity() + "\ncalculateInclination()="
				+ calculateInclination() + "\ncalculateDistance()="
				+ calculateDistance() + "\ncalculatePerigee()="
				+ calculatePerigee() + "\ncalculateApogee()="
				+ calculateApogee() + "\ncalculateSemimajorAxis()="
				+ calculateSemimajorAxis() + "\ncalculatePeriod()="
				+ calculatePeriod() + ""
						+ "\n" + satellite
						+ "\n" + body
						+ "]";
	}
}
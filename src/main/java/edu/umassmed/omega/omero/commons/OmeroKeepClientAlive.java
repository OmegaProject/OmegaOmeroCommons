/*******************************************************************************
 * Copyright (C) 2014 University of Massachusetts Medical School Alessandro
 * Rigano (Program in Molecular Medicine) Caterina Strambio De Castillia
 * (Program in Molecular Medicine)
 *
 * Created by the Open Microscopy Environment inteGrated Analysis (OMEGA) team:
 * Alex Rigano, Caterina Strambio De Castillia, Jasmine Clark, Vanni Galli,
 * Raffaello Giulietti, Loris Grossi, Eric Hunter, Tiziano Leidi, Jeremy Luban,
 * Ivo Sbalzarini and Mario Valle.
 *
 * Key contacts: Caterina Strambio De Castillia: caterina.strambio@umassmed.edu
 * Alex Rigano: alex.rigano@umassmed.edu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package edu.umassmed.omega.omero.commons;


/**
 * Keeps the services alive.
 */
class OmeroKeepClientAlive implements Runnable {
	
	/** Reference to the gateway. */
	private final OmeroGateway gateway;
	
	// TODO I should introduce a terminate here to avoid keeping alive when the
	// gateway changes?
	
	/**
	 * Creates a new instance.
	 *
	 * @param gateway
	 *            Reference to the gateway. Mustn't be <code>null</code>.
	 */
	public OmeroKeepClientAlive(final OmeroGateway gateway) {
		if (gateway == null)
			throw new IllegalArgumentException("No gateway specified.");
		this.gateway = gateway;
	}
	
	/** Runs. */
	@Override
	public void run() {
		// try {
		// synchronized (this.gateway) {
		// this.gateway.keepSessionAlive();
		// }
		// } catch (final Exception ex) {
		// OmegaLogFileManager.handleUncaughtException(ex, true);
		// // TODO handle differently
		// }
	}
}

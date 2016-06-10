/*
 * InitialCentroidChoiceStrategy.java
 *
 * Created: 2006-05-20
 *
 * Java Data Mining Framework (http://jdmf.sourceforge.net)
 * Copyright (C) 2006  Janusz Marchewa
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * $Author: quorthon $
 * $LastChangedRevision: 8 $
 * $LastChangedDate: 2006-07-25 22:53:46 +0200 (wto, 25 lip 2006) $
 */
package net.sf.jdmf.algorithms.clustering.centroid;

import java.util.List;
import java.util.Vector;

/**
 * Base interface for all initial centroid choice strategies.
 * 
 * @author quorthon
 */
public interface InitialCentroidChoiceStrategy {
    /**
     * Chooses initial centroids based on all points and the predicted number
     * of clusters.
     */
    List<Vector<Double>> chooseInitialCentroids( 
        List<Vector<Double>> points, Integer numberOfClusters );
}

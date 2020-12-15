/*-
 * #%L
 * SCIP: Single-cell image processing
 * %%
 * Copyright (C) 2020 Maxim Lippeveld
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package be.maximl.data.validators;

import be.maximl.data.Image;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import inra.ijpb.binary.conncomp.ConnectedComponentsLabeling;
import inra.ijpb.binary.conncomp.FloodFillComponentsLabeling;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import java.util.ArrayList;
import java.util.List;


public class ConnectedComponentsValidator<T extends RealType<T>, S extends NativeType<S>> implements Validator<T,S> {

    private int invalid = 0;
    final private List<Integer> invalidList = new ArrayList<>();

    @Override
    public boolean validate(Image<T,S> image) {
        ConnectedComponentsLabeling labeling = new FloodFillComponentsLabeling(1, 8);
        long[] dims = image.getDimensions();

        for (int i = 0; i<image.getChannels().size(); i++) {

            boolean[] mask = image.getMaskAsBooleanArray(i);
            byte[] maskBytes = new byte[mask.length];
            for (int b = 0; b<mask.length; b++) {
                maskBytes[b] = (byte)(mask[b]?1:0);
            }

            BinaryProcessor bp = new BinaryProcessor(new ByteProcessor((int)dims[0], (int)dims[1], maskBytes));
            int[] histogram = labeling.computeLabels(bp).getHistogram();

            boolean count = false;
            for (int j = 1; j<histogram.length; j++) { // start at 1 because 0 is background
                if (histogram[j] > 0) {
                    if (count) {
                        invalid++;
                        invalidList.add(image.getId());
                        return false;
                    }
                    count = true;
                }
            }
        }

        return true;
    }

    @Override
    public int getInvalidCount() {
        return invalid;
    }

    @Override
    public List<Integer> getInvalidIds() {
        return invalidList;
    }
}

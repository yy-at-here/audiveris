//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T i m e C o l u m n                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.time;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.score.TimeValue;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This abstract class provides the basis for management of a system-level column
 * of staff-level TimeBuilder instances since, within a system, a column of time
 * signatures must be complete and contain only identical signatures.
 * <p>
 * Subclasses:
 * <ul>
 * <li>{@link HeaderTimeColumn} works for system header.
 * <li>{@link BasicTimeColumn} works for time signatures found outside of system header.
 * </ul>
 */
public abstract class TimeColumn
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TimeColumn.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Containing system. */
    protected final SystemInfo system;

    /** Best time value found, if any. */
    protected TimeValue timeValue;

    /** Map of time builders. (one per staff) */
    protected final Map<Staff, TimeBuilder> builders = new TreeMap<>(Staff.byId);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>Column</code> object.
     *
     * @param system the underlying system
     */
    public TimeColumn (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Allocate instance of proper subclass of TimeBuilder
     *
     * @param staff the dedicated staff for this builder
     * @return the created TimeBuilder instance
     */
    protected abstract TimeBuilder allocateBuilder (Staff staff);

    //------------------//
    // checkConsistency //
    //------------------//
    /**
     * Use vertical redundancy within a system column of time signatures to come up
     * with the best selection.
     * <p>
     * The selection is driven from the whole system column point of view, as follows:
     * <ol>
     * <li>For each staff, identify all the possible and supported AbstractTimeInter
     * instances, each with its own grade.</li>
     * <li>Then for each possible AbstractTimeInter value (called TimeValue), make sure it
     * appears in each staff as a AbstractTimeInter instance and assign a global grade (as
     * average of staff-based AbstractTimeInter instances for the same TimeValue).</li>
     * <li>The best system-based TimeValue is then chosen as THE time signature for this
     * system column.</li>
     * <li>All staff non compatible AbstractTimeInter instances are destroyed and the member
     * numbers that don't belong to the chosen AbstractTimeInter are destroyed.</li>
     * </ol>
     *
     * @return true if OK, false otherwise
     */
    protected boolean checkConsistency ()
    {
        // Retrieve all time values found, organized by value and staff
        Map<TimeValue, AbstractTimeInter[]> vectors = getValueVectors();
        Map<TimeValue, Double> grades = new HashMap<>();

        TimeLoop:
        for (Map.Entry<TimeValue, AbstractTimeInter[]> entry : vectors.entrySet()) {
            final TimeValue time = entry.getKey();
            final AbstractTimeInter[] vector = entry.getValue();

            // Check that this time is present in enough standard staves
            // and compute the time mean grade
            double mean = 0;
            int count = 0;
            int standards = 0;

            final List<Staff> staves = system.getStaves();
            for (int idx = 0; idx < staves.size(); idx++) {
                final Staff staff = staves.get(idx);
                if (!staff.isTablature()) {
                    standards++;
                    final Inter inter = vector[idx];
                    if (inter == null) {
                        continue;
                    }

                    mean += inter.getGrade(); // TODO: use contextual?????
                    count++;
                }
            }

            if (count == 0) {
                continue TimeLoop;
            }

            mean /= count;

            // A value seen in too few staves is accepted only if it is a good reading on its own
            if ((count < minStaffCount(standards)) && (mean < minLoneGrade())) {
                logger.debug(
                        "System#{} TimeValue {} found in only {}/{} standard staves, grade {}",
                        system.getId(),
                        time,
                        count,
                        standards,
                        mean);

                continue TimeLoop;
            }

            grades.put(time, mean);
        }

        logger.debug("System#{} time sig grades {}", system.getId(), grades);

        // Select the best time value at system level
        double bestGrade = 0;

        for (Map.Entry<TimeValue, Double> entry : grades.entrySet()) {
            final double grade = entry.getValue();

            if (grade > bestGrade) {
                bestGrade = grade;
                timeValue = entry.getKey();
            }
        }

        if (timeValue == null) {
            return false; // Invalid column
        }

        // Forward the chosen time to each staff
        final AbstractTimeInter[] bestVector = vectors.get(timeValue);
        final List<Staff> staves = system.getStaves();

        // A model to copy into the staves that have no candidate of their own
        AbstractTimeInter model = null;

        if (replicateMissing()) {
            for (AbstractTimeInter t : bestVector) {
                if ((t != null) && !(t instanceof TimePairInter) && (t.getBounds() != null)) {
                    model = t;
                    break;
                }
            }
        }

        for (int is = 0; is < staves.size(); is++) {
            final Staff staff = staves.get(is);

            if (!staff.isTablature()) {
                TimeBuilder builder = builders.get(staff);
                AbstractTimeInter best = bestVector[is];

                if ((best == null) && (model != null)) {
                    best = replicateTo(model, staff);
                }

                builder.createTimeSig(best);
                builder.discardOthers();
            }
        }

        logger.debug("System#{} TimeSignature: {}", system.getId(), timeValue);

        return true;
    }

    /**
     * This is called when we discover that a column of candidate(s) is wrong,
     * so that all related data inserted in sig is removed.
     */
    protected abstract void cleanup ();

    //---------------//
    // minStaffCount //
    //---------------//
    /**
     * Report the minimum number of standard staves that must exhibit the same time value
     * for the column to be accepted.
     * <p>
     * Default is "all of them", which is what a system header column can guarantee since
     * every staff header is explicitly searched for a time signature.
     *
     * @param standards the number of standard (non tablature) staves in system
     * @return the minimum count of staves sharing the same time value
     */
    protected int minStaffCount (int standards)
    {
        return standards;
    }

    //-------------------//
    // minBuilderCount //
    //-------------------//
    /**
     * Report the minimum number of staves that must yield at least one candidate for the
     * column retrieval to go on at all.
     * <p>
     * Default is "all of them", as in a system header.
     *
     * @param standards the number of standard staves in system
     * @return the minimum count of staves with a candidate
     */
    protected int minBuilderCount (int standards)
    {
        return standards;
    }

    //---------------//
    // minLoneGrade //
    //---------------//
    /**
     * Report the minimum mean grade that lets a time value be accepted although it was found
     * in fewer staves than {@link #minStaffCount(int)}.
     * <p>
     * Default is unreachable, i.e. no such exception.
     *
     * @return the minimum mean grade
     */
    protected double minLoneGrade ()
    {
        return Double.MAX_VALUE;
    }

    //-------------------//
    // cleanupOnFailure //
    //-------------------//
    /**
     * Called when the column was built but not validated by {@link #checkConsistency()}.
     * <p>
     * Default is to keep the candidates in the SIG, as the header path does.
     */
    protected void cleanupOnFailure ()
    {
        // Void by default
    }

    //-------------------//
    // replicateMissing //
    //-------------------//
    /**
     * Report whether the winning time value should be copied into the staves of the system
     * that have no candidate of their own.
     * <p>
     * A time signature applies to the whole system, so a value accepted for the column must be
     * recorded in every staff: the exporter writes &lt;time&gt; per part, and the rhythm engine
     * derives the stack expected duration from the stack as a whole. Leaving a staff without it
     * makes that part's measure be truncated against a duration it never declared.
     * <p>
     * Not needed inside a system header, where every staff is searched explicitly.
     *
     * @return true to replicate
     */
    protected boolean replicateMissing ()
    {
        return false;
    }

    //--------------//
    // replicateTo //
    //--------------//
    /**
     * Create, in the provided staff, a copy of the provided time inter, vertically shifted to
     * that staff.
     *
     * @param model the time inter to copy
     * @param staff the target staff
     * @return the created copy, already added to the SIG
     */
    private AbstractTimeInter replicateTo (AbstractTimeInter model,
                                           Staff staff)
    {
        final AbstractTimeInter replica = model.replicate(staff);
        final Rectangle box = model.getBounds();
        final double x = box.getCenterX();
        final double dy = staff.getFirstLine().yAt(x) - model.getStaff().getFirstLine().yAt(x);
        final Rectangle target = new Rectangle(box);
        target.y += (int) Math.rint(dy);
        replica.setBounds(target);

        system.getSig().addVertex(replica);
        logger.debug("System#{} replicated {} into staff#{}", system.getId(), model, staff.getId());

        return replica;
    }

    //------------------//
    // discardNeighbors //
    //------------------//
    protected void discardNeighbors ()
    {
        final SIGraph sig = system.getSig();
        final Collection<AbstractTimeInter> times = getTimeInters().values();
        final Rectangle columnBox = Inters.getBounds(times);
        final List<Inter> neighbors = sig.inters(
                (Inter inter) -> inter.getBounds().intersects(columnBox)
                        && !(inter instanceof InterEnsemble));

        // Let's not consider our own time items as overlapping neighbors
        for (AbstractTimeInter time : times) {
            if (time instanceof TimePairInter pair) {
                neighbors.removeAll(pair.getMembers());
            }

            neighbors.remove(time);
        }

        for (AbstractTimeInter time : times) {
            final double timeGrade = time.getGrade();

            for (Iterator<Inter> it = neighbors.iterator(); it.hasNext();) {
                Inter neighbor = it.next();

                if (neighbor.overlaps(time) && (neighbor.getGrade() < timeGrade)) {
                    if (neighbor.isVip()) {
                        logger.info("VIP Deleting time overlapping {}", neighbor);
                    }
                    neighbor.remove();
                    it.remove();
                }
            }
        }
    }

    //---------------//
    // getTimeInters //
    //---------------//
    /**
     * Report the time inter instance for each staff in the column.
     *
     * @return the map: staff &rarr; time inter
     */
    public Map<Staff, AbstractTimeInter> getTimeInters ()
    {
        Map<Staff, AbstractTimeInter> times = new TreeMap<>(Staff.byId);

        for (Map.Entry<Staff, TimeBuilder> entry : builders.entrySet()) {
            final AbstractTimeInter timeInter = entry.getValue().getTimeInter();

            if (timeInter != null) {
                times.put(entry.getKey(), timeInter);
            }
        }

        return times;
    }

    /**
     * Report the system vector of values for each time value found.
     * A vector is an array, one element per staff, the element being the staff candidate
     * AbstractTimeInter for the desired time value, or null if the time value has no acceptable
     * candidate in this staff.
     *
     * @return the system vectors of candidates found, organized per TimeValue
     */
    protected Map<TimeValue, AbstractTimeInter[]> getValueVectors ()
    {
        // Retrieve all occurrences of time values across staves.
        final Map<TimeValue, AbstractTimeInter[]> values = new HashMap<>();

        // Loop on system staves
        final List<Staff> staves = system.getStaves();

        for (int index = 0; index < staves.size(); index++) {
            final Staff staff = staves.get(index);

            if (staff.isTablature()) {
                continue;
            }

            final TimeBuilder builder = builders.get(staff);
            final SIGraph sig = builder.sig;

            // Whole candidate signatures, if any, in this staff
            for (Inter inter : builder.wholes) {
                AbstractTimeInter whole = (AbstractTimeInter) inter;
                TimeValue time = whole.getValue();
                AbstractTimeInter[] vector = values.get(time);

                if (vector == null) {
                    values.put(time, vector = new AbstractTimeInter[staves.size()]);
                }

                if ((vector[index] == null) || (inter.getGrade() > vector[index].getGrade())) {
                    vector[index] = whole;
                }
            }

            // Num/Den pair candidate signatures, if any
            for (Inter nInter : builder.nums) {
                TimeNumberInter num = (TimeNumberInter) nInter;

                for (Relation rel : sig.getRelations(num, TimeTopBottomRelation.class)) {
                    TimeNumberInter den = (TimeNumberInter) sig.getOppositeInter(nInter, rel);
                    TimePairInter pair = TimePairInter.createAdded(num, den);
                    TimeValue time = pair.getValue();
                    AbstractTimeInter[] vector = values.get(time);

                    if (vector == null) {
                        values.put(time, vector = new AbstractTimeInter[staves.size()]);
                    }

                    if ((vector[index] == null) || (pair.getGrade() > vector[index].getGrade())) {
                        vector[index] = pair;
                    }
                }
            }
        }

        return values;
    }

    /**
     * Check that all candidates are vertically aligned.
     * When processing system header, candidates are aligned by construction.
     * But, outside headers, candidates within the same stack have to be checked for such
     * alignment.
     */
    protected void purgeUnaligned ()
    {
        // Void by default
    }

    //--------------//
    // retrieveTime //
    //--------------//
    /**
     * This is the main entry point for time signature, it retrieves the column of
     * staves candidates time signatures, and selects the best one at system level.
     *
     * @return 0 if valid, or -1 if invalid
     */
    public int retrieveTime ()
    {
        // Allocate one time-sig builder for each staff within system
        for (Staff staff : system.getStaves()) {
            if (!staff.isTablature()) {
                builders.put(staff, allocateBuilder(staff));
            }
        }

        // Process each staff on turn, to find candidates
        int okCount = 0;
        int standards = 0;

        for (TimeBuilder builder : builders.values()) {
            standards++;

            // Retrieve candidates for time items
            builder.findCandidates();

            // This fails if no candidate at all is kept in staff after filtering
            if (builder.filterCandidates()) {
                okCount++;
            }
        }

        if (okCount < minBuilderCount(standards)) {
            cleanup(); // Clean up what has been constructed

            return -1; // We failed to find a time sig in stack
        }

        // Check vertical alignment
        purgeUnaligned();

        // Check time sig consistency at system level
        if (checkConsistency()) {
            discardNeighbors();

            return 0;
        }

        cleanupOnFailure();

        return -1; // Failed
    }

    //----------------//
    // getMaxDxOffset //
    //----------------//
    /**
     * Report the maximum abscissa shift between de-skewed time items in column.
     *
     * @param sheet containing sheet
     * @return maximum abscissa shift
     */
    public static int getMaxDxOffset (Sheet sheet)
    {
        return sheet.getScale().toPixels(constants.maxDxOffset);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        private final Scale.Fraction maxDxOffset = new Scale.Fraction(
                2,
                "Maximum abscissa shift between deskewed time items in a column");
    }
}

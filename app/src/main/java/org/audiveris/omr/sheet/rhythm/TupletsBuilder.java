//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T u p l e t s B u i l d e r                                  //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>TupletsBuilder</code> tries to connect every tuplet symbol in a measure stack
 * to its embraced chords.
 *
 * @author Hervé Bitteur
 */
public class TupletsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TupletsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated measure stack. */
    private final MeasureStack stack;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>TupletsBuilder</code> object.
     *
     * @param stack the dedicated stack
     */
    public TupletsBuilder (MeasureStack stack)
    {
        this.stack = stack;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // getChordsAround //
    //-----------------//
    /**
     * Report the list of AbstractChordInter instances (rests & heads) in the
     * neighborhood of the specified Inter.
     * <p>
     * Neighborhood is limited horizontally by the measure sides and vertically by the staves above
     * and below.
     *
     * @param tuplet the inter of interest
     * @return the list of neighbors, sorted by euclidean distance to tuplet sign
     */
    private List<AbstractChordInter> getChordsAround (TupletInter tuplet)
    {
        final List<AbstractChordInter> chords = new ArrayList<>();
        final Point tupletCenter = tuplet.getCenter();

        if (stack == null) {
            return chords;
        }

        final List<Staff> stavesAround = stack.getSystem().getStavesAround(tupletCenter);
        logger.trace("{} around:{}", tuplet, stavesAround);

        for (AbstractChordInter chord : stack.getStandardChords()) {
            final List<Staff> chordStaves = new ArrayList<>(chord.getStaves());
            chordStaves.retainAll(stavesAround);

            if (!chordStaves.isEmpty()) {
                chords.add(chord);
            }
        }

        Collections.sort(chords, new ByEuclidean(tupletCenter));
        logger.trace("Chords: {}", Inters.ids(chords));

        return chords;
    }

    //------------------//
    // linkStackTuplets //
    //------------------//
    /**
     * Try to link all tuplet signs in stack.
     * <p>
     * A tuplet sign embraces a specific number of notes (heads / rests).
     * <p>
     * Its neighborhood is limited in its part, vertically to staff above and staff below and
     * horizontally to its containing measure stack.
     */
    public void linkStackTuplets ()
    {
        final Set<TupletInter> tuplets = stack.getTuplets();

        for (TupletInter tuplet : tuplets) {
            if (tuplet.isVip()) {
                logger.info("VIP linkStackTuplets for {}", tuplet);
            }

            // Purge existing tuplet-chord relations, except manual ones if any
            final SIGraph sig = stack.getSystem().getSig();
            final Set<Relation> rels = sig.getRelations(tuplet, ChordTupletRelation.class);

            for (Relation rel : rels) {
                if (!rel.isManual()) {
                    sig.removeEdge(rel);
                }
            }

            final Collection<Link> links = lookupLinks(tuplet);

            for (Link link : links) {
                link.applyTo(tuplet);
            }

            if (!tuplet.isManual() && !sig.hasRelation(tuplet, ChordTupletRelation.class)) {
                tuplet.remove();
            }
        }
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Look up for tuplet relevant chords.
     *
     * @param tuplet the tuplet sign
     * @return the collection of links found, perhaps empty
     */
    public Collection<Link> lookupLinks (TupletInter tuplet)
    {
        // Try to link tuplet with proper chords found in measure stack
        // (just staff above and staff below)
        List<AbstractChordInter> chordcandidates = getChordsAround(tuplet);

        // Now, get the properly embraced chords
        SortedSet<AbstractChordInter> chords = getEmbracedChords(tuplet, chordcandidates);

        if (chords == null) {
            return Collections.emptySet();
        }

        logger.trace("{} connectable to {}", tuplet, chords);

        List<Link> links = new ArrayList<>();

        for (AbstractChordInter chord : chords) {
            links.add(new Link(chord, new ChordTupletRelation(tuplet.getShape()), false));
        }

        return links;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------------//
    // expectedCount //
    //---------------//
    /**
     * Report the number of basic items governed by the tuplet.
     * A given chord may represent several basic items (chords of base duration)
     *
     * @param shape the tuplet shape
     * @return 3 or 6
     */
    private static int expectedCount (Shape shape)
    {
        return switch (shape) {
            case TUPLET_THREE -> 3;
            case TUPLET_SIX -> 6;
            default -> {
                logger.error("Incorrect tuplet shape {}", shape);
                yield 0;
            }
        };
    }

    //------------------------//
    // filterChordsOnAbscissa //
    //------------------------//
    /**
     * If two chords overlap horizontally, discard the one with higher vertical distance
     * from the tuplet sign.
     *
     * @param tuplet     underlying tuplet sign
     * @param candidates the chords candidates, ordered by euclidean distance to sign
     */
    private static void filterChordsOnAbscissa (TupletInter tuplet,
                                                List<AbstractChordInter> candidates)
    {
        final Point pt = tuplet.getCenter();

        for (int idx1 = 0; idx1 < candidates.size(); idx1++) {
            final AbstractChordInter ch1 = candidates.get(idx1);
            final Rectangle b1 = ch1.getBounds();
            final Point p1 = ch1.getCenter();

            for (int idx2 = idx1 + 1; idx2 < candidates.size(); idx2++) {
                final AbstractChordInter ch2 = candidates.get(idx2);
                final Rectangle b2 = ch2.getBounds();

                if (GeoUtil.xOverlap(b1, b2) > 0) {
                    // Discard ch1 or ch2, based on y-distance
                    final Point p2 = ch2.getCenter();
                    final int d1 = Math.abs(p1.y - pt.y);
                    final int d2 = Math.abs(p2.y - pt.y);

                    if (d1 < d2) {
                        candidates.remove(ch2);
                        idx2--;
                    } else {
                        candidates.remove(ch1);
                        idx1--;
                        break;
                    }
                }
            }
        }
    }

    //-------------------//
    // getEmbracedChords //
    //-------------------//
    /**
     * Report the proper collection of chords that are embraced by the tuplet.
     *
     * @param tuplet     underlying tuplet sign
     * @param candidates the chords candidates, ordered by euclidean distance to sign
     * @return the set of embraced chords, ordered from left to right, or null if retrieval failed
     */
    public static SortedSet<AbstractChordInter> getEmbracedChords (TupletInter tuplet,
                                                                   List<AbstractChordInter> candidates)
    {
        logger.trace("{} getEmbracedChords", tuplet);

        // Keep the unfiltered list: filterChordsOnAbscissa() may drop a genuine member of the
        // group in favour of a competing (often spurious) chord at the same abscissa.
        final List<AbstractChordInter> allCandidates = new ArrayList<>(candidates);

        filterChordsOnAbscissa(tuplet, candidates);

        // We consider each candidate in turn, with its duration
        // in order to determine the duration base of the tuplet
        final TupletCollector collector = new TupletCollector(
                tuplet,
                new TreeSet<>(Inters.byFullAbscissa));
        final Staff targetStaff = getTargetStaff(candidates);

        for (AbstractChordInter chord : candidates) {
            // We assume that chords with 2 staves have their tuplet sign above...
            Staff staff = chord.getTopStaff();

            // Check that all chords are on the same staff
            if (staff != targetStaff) {
                continue;
            }

            collector.include(chord);

            // Check we have collected the exact amount of time
            // TODO: Test is questionable for non-reliable candidates
            if (collector.isNotOk()) {
                logger.debug("{} {}", tuplet, collector.getStatusMessage());

                return lenientChords(tuplet, allCandidates, targetStaff);
            } else if (collector.isOk()) {
                if (logger.isDebugEnabled()) {
                    collector.dump();
                }

                return collector.getChords(); // Normal exit
            }
        }

        // Candidates are exhausted, we lack chords
        logger.debug("{} {}", tuplet, collector.getStatusMessage());

        return lenientChords(tuplet, allCandidates, targetStaff);
    }

    //---------------//
    // lenientChords //
    //---------------//
    /**
     * Fallback used when the strict duration-based retrieval has failed.
     * <p>
     * The strict collector requires the embraced chords to sum up to
     * <i>exactly</i> base * expectedCount, which a single mis-read duration
     * (spurious augmentation dot, missed beam, wrong head) is enough to defeat.
     * Here we rely on the printed grouping instead:
     * <ol>
     * <li>the beam group of the chord closest to the sign, if it holds exactly the
     * expected number of chords and its abscissa range brackets the sign,
     * <li>otherwise the window of expectedCount consecutive chords (on the target staff)
     * whose abscissa range brackets the sign and whose middle is closest to it.
     * </ol>
     *
     * @param tuplet      the tuplet sign
     * @param candidates  chord candidates (unfiltered), ordered by euclidean distance to sign
     * @param targetStaff the staff retained for this sign
     * @return the embraced chords, or null
     */
    private static SortedSet<AbstractChordInter> lenientChords (
            TupletInter tuplet,
            List<AbstractChordInter> candidates,
            Staff targetStaff)
    {
        if (!constants.lenientLinking.isSet() || (targetStaff == null)) {
            return null;
        }

        // A weak sign is more likely to be noise (fingering, dirt, a mis-read symbol) than a
        // real tuplet: for those, keep the strict behaviour (i.e. drop the sign).
        if (tuplet.getGrade() < constants.lenientMinGrade.getValue()) {
            logger.debug("{} too weak for lenient linking", tuplet);

            return null;
        }

        // A tuplet digit is a text-height glyph. A blob much shorter than an interline is ink
        // noise (a staff-line fragment between beams, a dot, ...) whatever the classifier says.
        final Rectangle signBox = tuplet.getBounds();
        final Scale scale = tuplet.getSig().getSystem().getSheet().getScale();

        if ((signBox == null) || (signBox.height < scale.toPixels(constants.minSignHeight))) {
            logger.debug("{} too small for lenient linking", tuplet);

            return null;
        }

        final int count = expectedCount(tuplet.getShape());

        if (count == 0) {
            return null;
        }

        final int signX = tuplet.getCenter().x;

        // Chords of the target staff, ordered by abscissa
        final List<AbstractChordInter> staffChords = new ArrayList<>();

        for (AbstractChordInter ch : candidates) {
            if (ch.getTopStaff() == targetStaff) {
                staffChords.add(ch);
            }
        }

        Collections.sort(staffChords, Inters.byFullAbscissa);

        // At a given abscissa a rest and a head cannot both be sounding in the same voice:
        // when they overlap, the head is the real event and the rest is a mis-read symbol
        // (very often the tuplet sign itself, read as a rest).
        for (int i = 0; i < staffChords.size(); i++) {
            final AbstractChordInter ch = staffChords.get(i);

            if (!ch.isRest()) {
                continue;
            }

            for (AbstractChordInter other : staffChords) {
                if ((other != ch) && !other.isRest() && (GeoUtil.xOverlap(
                        ch.getBounds(),
                        other.getBounds()) > 0)) {
                    staffChords.remove(i--);

                    break;
                }
            }
        }

        // 1/ Beam group of the chord closest to the sign
        for (AbstractChordInter ch : candidates) {
            if (ch.getTopStaff() != targetStaff) {
                continue;
            }

            final BeamGroupInter bg = ch.getBeamGroup();

            if (bg != null) {
                final List<AbstractChordInter> group = new ArrayList<>();

                for (AbstractChordInter g : bg.getChords()) {
                    if (staffChords.contains(g)) {
                        group.add(g);
                    }
                }

                Collections.sort(group, Inters.byFullAbscissa);

                if ((group.size() == count) && brackets(signX, group)) {
                    logger.debug("{} lenient beam-group link to {}", tuplet, Inters.ids(group));

                    final SortedSet<AbstractChordInter> set = new TreeSet<>(Inters.byFullAbscissa);
                    set.addAll(group);

                    return set;
                }
            }

            break; // Only the closest chord is considered for this rule
        }

        // 2/ Window of expectedCount consecutive chords bracketing the sign
        List<AbstractChordInter> best = null;
        int bestDist = Integer.MAX_VALUE;

        for (int i = 0; (i + count) <= staffChords.size(); i++) {
            final List<AbstractChordInter> win = staffChords.subList(i, i + count);

            if (brackets(signX, win)) {
                final int mid = (win.get(0).getTailLocation().x + win.get(count - 1)
                        .getTailLocation().x) / 2;
                final int dist = Math.abs(mid - signX);

                if (dist < bestDist) {
                    bestDist = dist;
                    best = new ArrayList<>(win);
                }
            }
        }

        if (best != null) {
            logger.debug("{} lenient window link to {}", tuplet, Inters.ids(best));

            final SortedSet<AbstractChordInter> set = new TreeSet<>(Inters.byFullAbscissa);
            set.addAll(best);

            return set;
        }

        return null;
    }

    //----------//
    // brackets //
    //----------//
    /**
     * Check that the sign abscissa lies between the tails of the first and last chords.
     *
     * @param signX abscissa of the tuplet sign
     * @param group the abscissa-ordered chords
     * @return true if the sign is bracketed by the group
     */
    private static boolean brackets (int signX,
                                     List<AbstractChordInter> group)
    {
        if (group.isEmpty()) {
            return false;
        }

        final int x1 = group.get(0).getTailLocation().x;
        final int x2 = group.get(group.size() - 1).getTailLocation().x;

        return (signX >= Math.min(x1, x2)) && (signX <= Math.max(x1, x2));
    }

    //----------------//
    // getTargetStaff //
    //----------------//
    /**
     * Report the staff of first head-based chord among the sorted candidates
     *
     * @param candidates candidates ordered by distance from tuplet
     * @return the target staff
     */
    private static Staff getTargetStaff (List<AbstractChordInter> candidates)
    {
        for (AbstractChordInter chord : candidates) {
            if (!chord.isRest()) {
                return chord.getTopStaff();
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // ByEuclidean //
    //-------------//
    private static class ByEuclidean
            implements Comparator<AbstractChordInter>
    {
        /** The location of the tuplet sign */
        private final Point signPoint;

        ByEuclidean (Point signPoint)
        {
            this.signPoint = signPoint;
        }

        /** Compare their euclidean distance from the signPoint reference */
        @Override
        public int compare (AbstractChordInter c1,
                            AbstractChordInter c2)
        {
            double dx1 = GeoUtil.ptDistanceSq(c1.getBounds(), signPoint.x, signPoint.y);
            double dx2 = GeoUtil.ptDistanceSq(c2.getBounds(), signPoint.x, signPoint.y);

            return Double.compare(dx1, dx2);
        }
    }

    //-----------------//
    // TupletCollector //
    //-----------------//
    private static class TupletCollector
    {
        /** Underlying sign. */
        private final TupletInter tuplet;

        /** The maximum number of base items expected. (TODO: this is not always true) */
        private final int expectedCount;

        /** The chords collected so far. */
        private final SortedSet<AbstractChordInter> chords;

        /** The base duration as identified so far. */
        private Rational base = Rational.MAX_VALUE;

        /** The total duration expected (using the known base). */
        private Rational expectedTotal = Rational.MAX_VALUE;

        /** The total duration so far. */
        private Rational total = Rational.ZERO;

        /** Current status. */
        private Status status;

        TupletCollector (TupletInter tuplet,
                         SortedSet<AbstractChordInter> chords)
        {
            this.tuplet = tuplet;
            expectedCount = expectedCount(tuplet.getShape());
            this.chords = chords;
        }

        /** Include a chord into the collection */
        private void doInclude (AbstractChordInter chord)
        {
            if (chords.add(chord)) {
                Rational sansTuplet = chord.getDurationSansTuplet();
                total = total.plus(sansTuplet);

                // If this is a shorter chord, let's update the base
                // TODO: this is not always true.
                if (sansTuplet.compareTo(base) < 0) {
                    base = sansTuplet;
                    expectedTotal = base.times(expectedCount);
                }
            }
        }

        public void dump ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(tuplet);

            sb.append(" ").append(status);

            sb.append(" Base:").append(base);

            sb.append(" ExpectedTotal:").append(expectedTotal);

            sb.append(" Total:").append(total);

            for (AbstractChordInter chord : chords) {
                sb.append("\n").append(chord);
            }

            logger.info(sb.toString());
        }

        /**
         * Retrieve all chords linked via the smallest beam to the provided chord.
         * <p>
         * We have to check position of tuplet with respect to chord
         * If tuplet is closer to chord tail side (thus beam) we use siblings
         * Otherwise, tuplet is closer to head and we don't propagate to beam siblings
         *
         * @param chord the provided chord
         * @return all sibling chords, including the chord provided
         */
        protected Set<AbstractChordInter> getBeamSiblings (AbstractChordInter chord)
        {
            final Set<AbstractChordInter> set = new LinkedHashSet<>();
            set.add(chord);

            final StemInter stem = chord.getStem();

            if (stem != null) {
                final Point center = tuplet.getCenter();
                final Point tail = chord.getTailLocation();
                final Point head = chord.getHeadLocation();

                if (center.distanceSq(tail) < center.distanceSq(head)) {
                    // Pick up the smallest beam, if any, connected to this stem
                    int smallestWidth = Integer.MAX_VALUE;
                    AbstractBeamInter smallestBeam = null;

                    for (AbstractBeamInter beam : stem.getBeams()) {
                        int width = beam.getBounds().width;

                        if (width < smallestWidth) {
                            smallestWidth = width;
                            smallestBeam = beam;
                        }
                    }

                    if (smallestBeam != null) {
                        for (StemInter s : smallestBeam.getStems()) {
                            set.addAll(s.getChords());
                        }
                    }
                }
            }

            return set;
        }

        public SortedSet<AbstractChordInter> getChords ()
        {
            return chords;
        }

        public String getStatusMessage ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(status).append(" sequence in ").append(tuplet.getShape()).append(": ").append(
                    total);

            if (expectedTotal != Rational.MAX_VALUE) {
                sb.append(" vs ").append(expectedTotal);
            }

            return sb.toString();
        }

        public Rational getTotal ()
        {
            return total;
        }

        /** Include the provided chord into the collection */
        public void include (AbstractChordInter chord)
        {
            if (!chords.contains(chord)) {
                // Chord together with its beam-siblings
                // (only if tuplet is on beam side of the stems)
                Set<AbstractChordInter> siblings = getBeamSiblings(chord);

                for (AbstractChordInter ch : siblings) {
                    doInclude(ch);
                }

                // Check count and duration
                // TODO: this is not always true, and thus should be refined!
                if (chords.size() > expectedCount) {
                    status = Status.TOO_MANY;
                } else if (total.compareTo(expectedTotal) > 0) {
                    status = Status.TOO_LONG;
                } else if (total.equals(expectedTotal)) {
                    // Check tuplet sign is within chords abscissae
                    if (isWithinChordsAbscissaRange()) {
                        status = Status.OK;
                    } else {
                        status = Status.OUTSIDE;
                    }
                }
            }
        }

        public boolean isNotOk ()
        {
            return (status != null) && (status != Status.OK);
        }

        public boolean isOk ()
        {
            return status == Status.OK;
        }

        /** Check whether the tuplet sign lies between the chords abscissae. */
        private boolean isWithinChordsAbscissaRange ()
        {
            int signX = tuplet.getCenter().x;

            return (signX >= chords.first().getTailLocation().x) && (signX <= chords.last()
                    .getTailLocation().x);
        }

        /** Describe the current status of the tuplet collector */
        public enum Status
        {
            TOO_SHORT,
            OK,
            TOO_LONG,
            TOO_MANY,
            OUTSIDE;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean lenientLinking = new Constant.Boolean(
                true,
                "Should a tuplet sign fall back to printed grouping when durations do not add up?");

        private final Constant.Ratio lenientMinGrade = new Constant.Ratio(
                0.40,
                "Minimum tuplet grade for the lenient (printed-grouping) fallback");

        private final Scale.Fraction minSignHeight = new Scale.Fraction(
                1.0,
                "Minimum height of a tuplet sign eligible for the lenient fallback");
    }
}

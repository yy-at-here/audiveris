//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r F a c t o r y                                    //
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.BOW_DOWN;
import static org.audiveris.omr.glyph.Shape.BOW_UP;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.ProcessingSwitch;
import org.audiveris.omr.sheet.ProcessingSwitches;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.time.BasicTimeColumn;
import org.audiveris.omr.sheet.time.TimeColumn;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractPauseInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BeatUnitInter;
import org.audiveris.omr.sig.inter.BowInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.BreathMarkInter;
import org.audiveris.omr.sig.inter.CaesuraInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.ClefInter.ClefKind;
import org.audiveris.omr.sig.inter.ClutterInter;
import org.audiveris.omr.sig.inter.CompoundNoteInter;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.FingeringInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.FretInter;
import org.audiveris.omr.sig.inter.GraceChordInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.MarkerInter;
import org.audiveris.omr.sig.inter.MeasureRepeatInter;
import org.audiveris.omr.sig.inter.MetronomeInter;
import org.audiveris.omr.sig.inter.MultipleRestInter;
import org.audiveris.omr.sig.inter.NumberInter;
import org.audiveris.omr.sig.inter.OctaveShiftInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.PlayingInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallBeamInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeCustomInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TremoloInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class <code>InterFactory</code> generates the inter instances corresponding to
 * to an acceptable symbol evaluation in a given system.
 * <p>
 * (Generally there is one inter instance per evaluation, an exception is the case of full time
 * signature which leads to upper plus lower number instances).
 *
 * @author Hervé Bitteur
 */
public class InterFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(InterFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** All system stems, ordered by abscissa. */
    private final List<Inter> systemStems;

    /** All system heads, ordered by abscissa. */
    private final List<Inter> systemHeads;

    /** All system notes (heads and rests), ordered by abscissa. */
    private List<Inter> systemNotes;

    /** All system (head of rest) chords, ordered by abscissa. */
    private final List<Inter> systemChords;

    /** All system head-based chords, ordered by abscissa. */
    private final List<Inter> systemHeadChords;

    /** All system rests, ordered by abscissa. */
    private List<Inter> systemRests;

    /** All system bar lines, ordered by abscissa. */
    private List<Inter> systemBars;

    /** Dot factory companion. */
    private final DotFactory dotFactory;

    /** Processing switches. */
    private final ProcessingSwitches switches;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new InterFactory object.
     *
     * @param system the dedicated system
     */
    public InterFactory (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();

        systemStems = sig.inters(Shape.STEM);
        Collections.sort(systemStems, Inters.byAbscissa);

        systemHeads = sig.inters(HeadInter.class);
        Collections.sort(systemHeads, Inters.byAbscissa);

        systemChords = sig.inters(AbstractChordInter.class);
        Collections.sort(systemChords, Inters.byAbscissa);

        systemHeadChords = sig.inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        dotFactory = new DotFactory(this, system);

        switches = system.getSheet().getStub().getProcessingSwitches();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // create //
    //--------//
    /**
     * Create and add to SIG the proper inter instance(s) for provided evaluated glyph.
     *
     * @param eval         evaluation result
     * @param glyph        evaluated glyph
     * @param closestStaff only the closest staff, ordinate-wise
     * @return the created inter, perhaps null
     */
    public Inter create (Evaluation eval,
                         Glyph glyph,
                         Staff closestStaff)
    {
        final Inter inter = doCreate(eval, glyph, closestStaff);

        // Add to SIG if not already done
        if ((inter != null) && (inter.getSig() == null)) {
            sig.addVertex(inter);
        }

        if ((inter instanceof ClefInter clefInter) && (glyph != null)) {
            protectMidSystemClef(clefInter, eval, glyph);
        }

        return inter;
    }

    //----------------------//
    // protectMidSystemClef //
    //----------------------//
    /**
     * Give a clef change printed inside a system the two things it needs to survive:
     * the ink the HEADS step took from it, and a contextual grade above the generic floor.
     * <p>
     * A clef change printed inside a system is usually engraved at cue size, and the bowls of a
     * cue-size C clef are then read as stem-less note heads by the HEADS step. Head and clef
     * cover the very same ink, so {@link org.audiveris.omr.sig.SigReducer} puts them in mutual
     * exclusion and keeps whichever has the higher contextual grade - which, for a cue-size
     * clef, is regularly the head. Measured on a 600 dpi scan of a string quartet, a correct
     * cue C clef graded 0.494 against two spurious WHOLE_NOTE heads graded 0.510 and 0.528:
     * the clef lost the exclusion by 0.02 and the part stayed in the wrong clef, a fifth off,
     * for the rest of the system.
     * <p>
     * A head which lies <b>wholly inside</b> the clef glyph, belongs to the stem-less family
     * and carries <b>no stem and no slur or tie</b> has no evidence of its own: every pixel it
     * claims is also claimed by the clef, and the clef is the reading the classifier is
     * confident about. Such heads are removed, so that no exclusion is created at all.
     * <p>
     * A printed whole-note double stop is a different population and is not at risk: its heads
     * are not inside anything the classifier reads as a clef. On the same four pages, the six
     * printed whole-note double stops yield no clef evaluation whatsoever, their best clef
     * grade being 0.030 - more than ten times below {@link Constants#minClefGrade}.
     *
     * @param clef  the clef candidate just created
     * @param eval  the evaluation it comes from
     * @param glyph the underlying glyph
     */
    private void protectMidSystemClef (ClefInter clef,
                                       Evaluation eval,
                                       Glyph glyph)
    {
        if (!constants.protectMidSystemClef.isSet()) {
            return;
        }

        final Staff staff = clef.getStaff();

        if ((staff == null) || staff.isTablature()) {
            return;
        }

        // Beyond the system header only: a header clef is ClefBuilder's business
        if (glyph.getCenter2D().getX() <= system.getHeaderStop()) {
            return;
        }

        // The classifier must be confident that this ink is a clef
        if (eval.grade < constants.minClefGrade.getValue()) {
            return;
        }

        if (!hasClefGeometry(clef, glyph, staff)) {
            return;
        }

        // Gather the heads wholly inside the clef.
        // If any of them has evidence of its own, the ink is contested: leave the conflict
        // to SigReducer rather than deciding it here.
        final Rectangle box = glyph.getBounds();
        final List<Inter> victims = new ArrayList<>();

        for (Inter head : sig.inters(HeadInter.class)) {
            if (head.isRemoved() || !box.contains(head.getBounds())) {
                continue;
            }

            if (!ShapeSet.StemLessHeads.contains(head.getShape())) {
                return;
            }

            for (Relation rel : sig.edgesOf(head)) {
                if ((rel instanceof HeadStemRelation) || (rel instanceof SlurHeadRelation)) {
                    return;
                }
            }

            victims.add(head);
        }

        for (Inter victim : victims) {
            logger.info("Clef {} claims the ink of {}", clef, victim);
            victim.remove();
        }

        // Second half of the problem, and the one that actually decides the outcome.
        // A clef owns no relation that could raise its contextual grade - unlike a head (its
        // stem), an alter (its head) or a time number (its pair) - so it keeps the modest
        // intrinsic grade the classifier gave it, and SIGraph.deleteWeakInters removes every
        // inter below Grades.minContextualGrade (0.5). A cue-size clef is therefore deleted on
        // grade alone, whatever happens with the heads: measured on the same scan, the page-2
        // cue tenor clef enters the SIG at 0.518 and survives, the page-3 one enters at 0.395
        // and is purged. A candidate that has passed every gate above is lifted to that floor.
        final Double grade = clef.getGrade();

        if ((grade != null) && (grade < Grades.minContextualGrade)) {
            logger.info("Clef {} lifted from {} to the weak-inter floor", clef, grade);
            clef.setGrade(Grades.minContextualGrade);
        }
    }

    //-----------------//
    // hasClefGeometry //
    //-----------------//
    /**
     * Check that the glyph has the size and, for a C clef, the vertical placement of a clef.
     * <p>
     * The range runs from a cue-size C clef (measured: 2.75 to 2.78 interlines high, 1.89 to
     * 1.92 wide) up to a full-size G clef (about 7 interlines high). It is a sanity envelope,
     * not the discriminator: that role belongs to the classifier grade.
     *
     * @param clef  the clef candidate
     * @param glyph its underlying glyph
     * @param staff the related staff
     * @return true if the glyph could be a clef of that kind at that place
     */
    private boolean hasClefGeometry (ClefInter clef,
                                     Glyph glyph,
                                     Staff staff)
    {
        final int interline = staff.getSpecificInterline();
        final double normHeight = glyph.getHeight() / (double) interline;
        final double normWidth = glyph.getWidth() / (double) interline;

        if ((normHeight < constants.minClefHeight.getValue()) //
                || (normHeight > constants.maxClefHeight.getValue()) //
                || (normWidth < constants.minClefWidth.getValue()) //
                || (normWidth > constants.maxClefWidth.getValue())) {
            return false;
        }

        // A C clef is symmetric about its reference line, so its center must sit on a line
        if (clef.getShape() == Shape.C_CLEF) {
            final Double pp = system.estimatedPitch(glyph.getCenter2D());

            if (pp == null) {
                return false;
            }

            if (Math.abs(pp - (2 * Math.rint(pp / 2))) > constants.maxLinePitchOffset.getValue()) {
                return false;
            }
        }

        return true;
    }

    //-----------------------//
    // protectReturningClefs //
    //-----------------------//
    /**
     * Second pass, once every symbol of the system has been proposed: protect the clef that
     * <b>returns</b> a staff to the clef printed in its header.
     * <p>
     * Recovering a clef change without recovering the change that cancels it is worse than
     * recovering neither: the staff then stays in the new clef until the end of the system and
     * every note after the cancellation point is transposed. Measured on page 3 of a 600 dpi
     * scan of a string quartet, protecting the cue tenor clef alone put 3 discrepancy rows
     * right and 8 pitches wrong, because the bass clef printed two bars later - a cue-size
     * F_CLEF_SMALL graded 0.2444 - stayed below the leaving threshold and was still purged.
     * <p>
     * The returning direction can afford a lower bar than the leaving one, because it is the
     * safe direction: a spurious clef that merely restores what the header already says costs
     * nothing, while a spurious clef that takes a staff away transposes it. So a candidate is
     * lifted here when it installs exactly the {@link ClefInter.ClefKind} of the staff header,
     * when a <b>different</b> clef is actually in force at that abscissa - a clef that has
     * itself survived, so it is a real change and not a stray candidate - and when it passes
     * the same geometric gates. A restoring candidate with nothing to restore is left alone:
     * it carries no information and would only add risk.
     */
    public void protectReturningClefs ()
    {
        if (!constants.protectMidSystemClef.isSet()) {
            return;
        }

        for (Staff staff : system.getStaves()) {
            if (staff.isTablature()) {
                continue;
            }

            ClefInter header = null;
            final List<Inter> beyond = new ArrayList<>();

            for (Inter inter : sig.inters(ClefInter.class)) {
                if (inter.getStaff() != staff) {
                    continue;
                }

                if (inter.getCenter2D().getX() <= system.getHeaderStop()) {
                    header = (ClefInter) inter;
                } else {
                    beyond.add(inter);
                }
            }

            if ((header == null) || beyond.isEmpty()) {
                continue;
            }

            Collections.sort(beyond, Inters.byAbscissa);

            final ClefKind headerKind = header.getKind();
            ClefKind inForce = headerKind;

            for (Inter inter : beyond) {
                final ClefInter clef = (ClefInter) inter;
                final ClefKind kind = clef.getKind();
                final Glyph clefGlyph = clef.getGlyph();
                Double grade = clef.getGrade();

                if ((kind == null) || (grade == null) || (kind == inForce)) {
                    continue;
                }

                if ((kind == headerKind) //
                        && (grade < Grades.minContextualGrade) //
                        && (grade >= Grades.intrinsicRatio * constants.minReturningClefGrade
                                .getValue()) //
                        && (clefGlyph != null) //
                        && hasClefGeometry(clef, clefGlyph, staff)) {
                    logger.info(
                            "Returning clef {} lifted from {} to the weak-inter floor",
                            clef,
                            grade);
                    clef.setGrade(Grades.minContextualGrade);
                    grade = clef.getGrade();
                }

                // Only a clef that will survive the weak purge actually changes what is in force
                if (grade >= Grades.minContextualGrade) {
                    inForce = kind;
                }
            }
        }
    }

    //----------//
    // doCreate //
    //----------//
    /**
     * Create proper inter instance(s) for provided evaluated glyph.
     * <p>
     * This method addresses only the symbols handled via a classifier.
     * In current OMR design, this does <b>not</b> address:
     * <ul>
     * <li>Brace
     * <li>Bracket
     * <li>Barline
     * <li>Beam, Beam_hook
     * <li>Head
     * <li>Ledger
     * <li>Stem
     * <li>Curve
     * <li>Text, Lyrics
     * </ul>
     *
     * @param eval         evaluation result
     * @param glyph        evaluated glyph
     * @param closestStaff only the closest staff, ordinate-wise
     * @return the created inter or null
     */
    private Inter doCreate (Evaluation eval,
                            Glyph glyph,
                            Staff closestStaff)
    {
        final Shape shape = eval.shape;
        final double grade = Grades.intrinsicRatio * eval.grade;

        if (glyph.isVip()) {
            logger.info("VIP glyph#{} symbol created as {}", glyph.getId(), eval.shape);
        }

        switch (shape) {
            case CLUTTER:
                return null;

            // Octave shift
            case OTTAVA:
            case QUINDICESIMA:
            case VENTIDUESIMA:
                // Staff is very questionable!
                return OctaveShiftInter.create(glyph, shape, grade, closestStaff);

            // All dots:
            // - REPEAT_DOT
            // - STACCATO_DOT
            // - AUGMENTATION_DOT
            case DOT_set:
                dotFactory.instantDotChecks(eval, glyph, closestStaff);

                return null;

            // Measure repeat signs
            case REPEAT_ONE_BAR:
            case REPEAT_TWO_BARS:
            case REPEAT_FOUR_BARS:
                return MeasureRepeatInter.create(glyph, shape, grade, closestStaff); // Staff is OK

            // Clefs
            case G_CLEF:
            case G_CLEF_SMALL:
            case G_CLEF_8VA:
            case G_CLEF_8VB:
            case F_CLEF:
            case F_CLEF_SMALL:
            case F_CLEF_8VA:
            case F_CLEF_8VB:
            case C_CLEF:
            case PERCUSSION_CLEF:
                return ClefInter.createValid(glyph, shape, grade, closestStaff); // Staff is OK

            // Key signatures
            case KEY_FLAT_7:
            case KEY_FLAT_6:
            case KEY_FLAT_5:
            case KEY_FLAT_4:
            case KEY_FLAT_3:
            case KEY_FLAT_2:
            case KEY_FLAT_1:
            case KEY_CANCEL:
            case KEY_SHARP_1:
            case KEY_SHARP_2:
            case KEY_SHARP_3:
            case KEY_SHARP_4:
            case KEY_SHARP_5:
            case KEY_SHARP_6:
            case KEY_SHARP_7:
                return new KeyInter(grade, shape);

            // Time sig
            //        case TIME_ZERO:
            //        case TIME_ONE:
            case TIME_TWO:
            case TIME_THREE:
            case TIME_FOUR:
            case TIME_FIVE:
            case TIME_SIX:
            case TIME_SEVEN:
            case TIME_EIGHT:
            case TIME_NINE:
            case TIME_TWELVE:
            case TIME_SIXTEEN:
                // NOTA: These shapes are generally used for a time number or for a measure count.
                // At this point, we simply return a general-purpose number
                return NumberInter.create(glyph, shape, grade, closestStaff);

            case COMMON_TIME:
            case CUT_TIME:
            case TIME_FOUR_FOUR:
            case TIME_TWO_TWO:
            case TIME_TWO_FOUR:
            case TIME_THREE_FOUR:
            case TIME_FIVE_FOUR:
            case TIME_SIX_FOUR:
            case TIME_THREE_EIGHT:
            case TIME_SIX_EIGHT:
            case TIME_TWELVE_EIGHT:
                return TimeWholeInter.create(glyph, shape, grade, closestStaff); // Staff is OK

            // Flags
            case FLAG_1:
            case FLAG_2:
            case FLAG_3:
            case FLAG_4:
            case FLAG_5:
            case FLAG_1_DOWN:
            case FLAG_2_DOWN:
            case FLAG_3_DOWN:
            case FLAG_4_DOWN:
            case FLAG_5_DOWN:
            case SMALL_FLAG:
            case SMALL_FLAG_DOWN:
            case SMALL_FLAG_SLASH:
            case SMALL_FLAG_SLASH_DOWN:
                return AbstractFlagInter.createValidAdded(glyph, shape, grade, system, systemStems); // Glyph is checked

            // Rests
            case LONG_REST:
            case BREVE_REST:
            case WHOLE_REST:
            case HALF_REST:
            case QUARTER_REST:
            case EIGHTH_REST:
            case ONE_16TH_REST:
            case ONE_32ND_REST:
            case ONE_64TH_REST:
            case ONE_128TH_REST:
                return RestInter.createValid(glyph, shape, grade, system, systemHeadChords);

            // Tuplets
            case TUPLET_THREE:
            case TUPLET_SIX:
                return TupletInter.createValid(glyph, shape, grade, system, systemChords);

            // Accidentals
            case FLAT:
            case NATURAL:
            case SHARP:
            case DOUBLE_SHARP:
            case DOUBLE_FLAT: {
                // Staff is very questionable!
                if (closestStaff.isTablature()) {
                    return null;
                }

                AlterInter alter = AlterInter.create(glyph, shape, grade, closestStaff);

                sig.addVertex(alter);
                alter.setLinks(systemHeads);

                return alter;
            }

            // Articulations
            case ACCENT:
            case TENUTO:
            case STACCATO:
            case STACCATISSIMO:
            case STACCATISSIMO_BELOW:
            case MARCATO:
            case MARCATO_BELOW:
                return switches.getValue(ProcessingSwitch.articulations) ? ArticulationInter
                        .createValidAdded(glyph, shape, grade, system, systemHeadChords) : null;

            // Markers
            case CODA:
            case SEGNO:
            case DAL_SEGNO:
            case DA_CAPO: {
                MarkerInter marker = MarkerInter.create(glyph, shape, grade, closestStaff); // OK

                sig.addVertex(marker);
                marker.linkWithStaffBarline();

                return marker;
            }

            // Holds
            case FERMATA:
            case FERMATA_BELOW:
                return FermataInter.createValidAdded(glyph, shape, grade, system);

            // Pauses
            case CAESURA:
            case BREATH_MARK:
                return AbstractPauseInter.createValidAdded(
                        glyph,
                        shape,
                        grade,
                        closestStaff,
                        systemHeadChords);

            // Dynamics
            case DYNAMICS_P:
            case DYNAMICS_PP:
            case DYNAMICS_PPP:
            case DYNAMICS_MP:
            case DYNAMICS_F:
            case DYNAMICS_FF:
            case DYNAMICS_FFF:
            case DYNAMICS_MF:
            case DYNAMICS_FP:
            case DYNAMICS_FZ:
            case DYNAMICS_SF:
            case DYNAMICS_SFZ:
                return new DynamicsInter(glyph, shape, grade);

            // Wedges
            case CRESCENDO:
            case DIMINUENDO:
                return new WedgeInter(glyph, shape, grade);

            // Grace notes
            case GRACE_NOTE:
            case GRACE_NOTE_DOWN:
            case GRACE_NOTE_SLASH:
            case GRACE_NOTE_SLASH_DOWN:
                return GraceChordInter.createValidAdded(
                        glyph,
                        shape,
                        grade,
                        system,
                        systemHeadChords);

            // Ornaments
            case TR:
            case TURN:
            case TURN_INVERTED:
            case TURN_UP:
            case TURN_SLASH:
            case MORDENT:
            case MORDENT_INVERTED:
                return OrnamentInter.createValidAdded(
                        glyph,
                        shape,
                        grade,
                        system,
                        systemHeadChords);

            // Tremolos
            case TREMOLO_1:
            case TREMOLO_2:
            case TREMOLO_3:
                return switches.getValue(ProcessingSwitch.tremolos) ? TremoloInter.createValidAdded(
                        glyph,
                        shape,
                        grade,
                        system,
                        systemStems,
                        systemHeads) : null;

            // Plucked techniques
            case ARPEGGIATO:
                return ArpeggiatoInter.createValidAdded(glyph, grade, system, systemHeadChords);

            // Strings techniques
            case BOW_DOWN:
            case BOW_UP:
                return BowInter.createValidAdded(glyph, shape, grade, system, systemHeadChords);

            // Keyboards
            case PEDAL_MARK:
            case PEDAL_UP_MARK:
                return new PedalInter(glyph, shape, grade);

            // Fingering
            case DIGIT_0:
            case DIGIT_1:
            case DIGIT_2:
            case DIGIT_3:
            case DIGIT_4:
            case DIGIT_5:
                // Check potential link with proper note head
                return switches.getValue(ProcessingSwitch.fingerings) ? FingeringInter
                        .createValidAdded(glyph, shape, grade, system, systemHeadChords) : null;

            // Plucking
            case PLUCK_P:
            case PLUCK_I:
            case PLUCK_M:
            case PLUCK_A:
                // Check potential link with proper note head
                return switches.getValue(ProcessingSwitch.pluckings) ? PluckingInter
                        .createValidAdded(glyph, shape, grade, system, systemHeadChords) : null;

            // Romans
            case ROMAN_I:
            case ROMAN_II:
            case ROMAN_III:
            case ROMAN_IV:
            case ROMAN_V:
            case ROMAN_VI:
            case ROMAN_VII:
            case ROMAN_VIII:
            case ROMAN_IX:
            case ROMAN_X:
            case ROMAN_XI:
            case ROMAN_XII:
                return switches.getValue(ProcessingSwitch.frets) ? new FretInter(
                        glyph,
                        shape,
                        grade) : null;

            // Playings
            case PLAYING_OPEN:
            case PLAYING_HALF_OPEN:
            case PLAYING_CLOSED:
                // Check potential link with proper drum note
                return switches.getValue(ProcessingSwitch.drumNotation) ? PlayingInter
                        .createValidAdded(glyph, shape, grade, system, systemHeadChords) : null;

            // Others
            default:
                logger.debug("No support yet for {} glyph#{}", shape, glyph.getId());

                return null;
        }
    }

    //---------------//
    // getSystemBars //
    //---------------//
    /**
     * Report all system barlines.
     *
     * @return all barlines in the containing system
     */
    public List<Inter> getSystemBars ()
    {
        if (systemBars == null) {
            systemBars = sig.inters(BarlineInter.class);
            Collections.sort(systemBars, Inters.byAbscissa);
        }

        return systemBars;
    }

    //-----------------//
    // getSystemChords //
    //-----------------//
    List<Inter> getSystemChords ()
    {
        return systemChords;
    }

    //---------------------//
    // getSystemHeadChords //
    //---------------------//
    List<Inter> getSystemHeadChords ()
    {
        return systemHeadChords;
    }

    //----------------//
    // getSystemHeads //
    //----------------//
    List<Inter> getSystemHeads ()
    {
        return systemHeads;
    }

    //----------------//
    // getSystemNotes //
    //----------------//
    List<Inter> getSystemNotes ()
    {
        if (systemNotes == null) {
            systemNotes = new ArrayList<>(getSystemHeads().size() + getSystemRests().size());
            systemNotes.addAll(getSystemHeads());
            systemNotes.addAll(getSystemRests());
            Collections.sort(systemNotes, Inters.byAbscissa);
        }

        return systemNotes;
    }

    //----------------//
    // getSystemRests //
    //----------------//
    /**
     * Report all rests in system.
     *
     * @return all rests in the containing system
     */
    public List<Inter> getSystemRests ()
    {
        if (systemRests == null) {
            systemRests = sig.inters(RestInter.class);
            Collections.sort(systemRests, Inters.byAbscissa);
        }

        return systemRests;
    }

    //-----------------------//
    // handleComplexDynamics //
    //-----------------------//
    /**
     * Handle competition between complex and shorter dynamics.
     */
    private void handleComplexDynamics ()
    {
        // All dynamics in system
        final List<Inter> dynamics = sig.inters(DynamicsInter.class);

        // Complex dynamics in system, sorted by decreasing length
        final List<DynamicsInter> complexes = new ArrayList<>();

        for (Inter inter : dynamics) {
            DynamicsInter dyn = (DynamicsInter) inter;

            if (dyn.getSymbolString().length() > 1) {
                complexes.add(dyn);
            }
        }

        // Sort by decreasing length, then decreasing grade
        Collections.sort(
                complexes,
                (d1,
                 d2) -> {
                    final int lgComp = Integer.compare(
                            d2.getSymbolString().length(),
                            d1.getSymbolString().length());
                    if (lgComp != 0) {
                        return lgComp;
                    }
                    return Double.compare(d2.getGrade(), d1.getGrade());
                });

        for (DynamicsInter complex : complexes) {
            complex.swallowShorterDynamics(dynamics);
        }
    }

    //-------------//
    // handleTimes //
    //-------------//
    /**
     * Handle time inters outside of system header.
     * <p>
     * Isolated time inters found outside of system header lead to the retrieval of a column of
     * time signatures.
     */
    private void handleTimes ()
    {
        // Retrieve all time inters (outside staff headers)
        final List<Inter> systemTimes = sig.inters(
                new Class[] { TimeWholeInter.class, // Whole symbol like C or predefined 6/8
                        TimeCustomInter.class, // User modifiable combo 6/8
                        TimeNumberInter.class }); // Partial symbol like 6 or 8

        final List<Inter> headerTimes = new ArrayList<>();

        for (Inter inter : systemTimes) {
            Staff staff = inter.getStaff();

            if (inter.getCenter().x < staff.getHeaderStop()) {
                headerTimes.add(inter);
            }
        }

        systemTimes.removeAll(headerTimes);

        // Seed TimeNumberInter candidates from the general-purpose NumberInter instances.
        // Outside of a system header, a TIME_TWO..TIME_SIXTEEN glyph is created as a plain
        // NumberInter (see doCreate) and NumberInter.searchLinks can only pair a number with an
        // *already existing* TimeNumberInter. As only HeaderTimeBuilder ever creates one, a
        // numeric time signature standing further down the system could never be assembled.
        // So we promote here every number that sits inside a staff at a plausible time pitch,
        // and let the time column below validate or discard the candidates.
        final Map<Inter, Inter> promoted = new LinkedHashMap<>(); // timeNumber -> source number

        for (Inter inter : sig.inters(NumberInter.class)) {
            final NumberInter number = (NumberInter) inter;
            final Staff staff = number.getStaff();
            final Glyph glyph = number.getGlyph();

            if ((staff == null) || (glyph == null) || staff.isTablature()) {
                continue;
            }

            if (number.getCenter().x < staff.getHeaderStop()) {
                continue;
            }

            if (!staff.contains(number.getCenter())) {
                continue; // Likely a measure count, handled by SymbolsLinker
            }

            final TimeNumberInter tn = TimeNumberInter.create(
                    glyph,
                    number.getShape(),
                    number.getGrade(),
                    staff);

            if (tn != null) {
                sig.addVertex(tn);
                promoted.put(tn, number);
                systemTimes.add(tn);
                logger.debug("TIMEDBG promoted {} from {} staff#{}", tn, number, staff.getId());
            }
        }

        if (systemTimes.isEmpty()) {
            return;
        }

        // Dispatch these time inters into their containing stack
        final Map<MeasureStack, Set<Inter>> timeMap = new TreeMap<>(
                (s1,
                 s2) -> Integer.compare(s1.getIdValue(), s2.getIdValue()));

        for (Inter inter : systemTimes) {
            final MeasureStack stack = system.getStackAt(inter.getCenter());

            if (stack != null) {
                Set<Inter> stackSet = timeMap.get(stack);

                if (stackSet == null) {
                    timeMap.put(stack, stackSet = new LinkedHashSet<>());
                }

                stackSet.add(inter);
            }
        }

        // Finally, scan each stack populated with some time sig(s)
        for (Entry<MeasureStack, Set<Inter>> entry : timeMap.entrySet()) {
            final MeasureStack stack = entry.getKey();
            final TimeColumn column = new BasicTimeColumn(stack, entry.getValue());
            final int res = column.retrieveTime();
            logger.debug(
                    "TIMEDBG system#{} stack#{} candidates={} res={}",
                    system.getId(),
                    stack.getIdValue(),
                    entry.getValue(),
                    res);

            if (res == -1) {
                // Column was not validated: drop the candidates we seeded for this stack,
                // together with any TimePairInter that getValueVectors() may have built on them
                for (Inter inter : entry.getValue()) {
                    if (promoted.containsKey(inter) && !inter.isRemoved()) {
                        discardCandidate(inter);
                    }
                }
            }

            // If the stack does have a validated time sig, discard overlapping stuff right now!
            if (res != -1) {
                final Collection<AbstractTimeInter> times = column.getTimeInters().values();
                final Rectangle columnBox = Inters.getBounds(times);
                final List<Inter> neighbors = sig.inters(
                        (inter) -> inter.getBounds().intersects(columnBox)
                                && !(inter instanceof InterEnsemble));

                neighbors.removeAll(times);

                for (AbstractTimeInter time : times) {
                    final double timeGrade = time.getGrade();

                    for (Iterator<Inter> it = neighbors.iterator(); it.hasNext();) {
                        final Inter neighbor = it.next();

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
        }

        // Reconcile the promoted candidates with their source numbers
        for (Entry<Inter, Inter> e : promoted.entrySet()) {
            final Inter timeNumber = e.getKey();
            final Inter number = e.getValue();

            if (timeNumber.isRemoved()) {
                continue; // Source number survives, SymbolsLinker will deal with it
            }

            if (timeNumber.getEnsemble() != null) {
                // Validated as part of a time pair: the source number is now redundant
                if (!number.isRemoved()) {
                    number.remove();
                }
            } else {
                // Orphan candidate (no stack found, or column silently failed)
                discardCandidate(timeNumber);
            }
        }
    }

    //------------------//
    // discardCandidate //
    //------------------//
    /**
     * Remove a rejected time-number candidate, together with any ensemble built on it.
     *
     * @param candidate the candidate to remove
     */
    private void discardCandidate (Inter candidate)
    {
        for (Inter ensemble : candidate.getAllEnsembles()) {
            if (!ensemble.isRemoved()) {
                ensemble.remove();
            }
        }

        if (!candidate.isRemoved()) {
            candidate.remove();
        }
    }

    //------------//
    // lateChecks //
    //------------//
    /**
     * Perform late checks.
     */
    public void lateChecks ()
    {
        // Conflicting dot interpretations
        dotFactory.lateDotChecks();

        // Complex dynamics
        handleComplexDynamics();

        // Column consistency of Time Signatures in a system
        handleTimes();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------------//
    // createManual //
    //--------------//
    /**
     * Create a manual inter instance to handle the provided shape.
     *
     * @param shape provided shape
     * @param sheet related sheet
     * @return the created manual inter or null
     */
    public static Inter createManual (Shape shape,
                                      Sheet sheet)
    {
        Inter ghost = doCreateManual(shape, sheet);

        if (ghost != null) {
            ghost.setManual(true);
        }

        return ghost;
    }

    //----------------//
    // doCreateManual //
    //----------------//
    /**
     * Create a not-yet-settled inter instance to handle the provided shape.
     * <p>
     * This method is meant to address all shapes for which a manual inter can be created.
     *
     * @param shape provided shape
     * @param sheet related sheet
     * @return the created ghost inter or null
     */
    private static Inter doCreateManual (Shape shape,
                                         Sheet sheet)
    {
        final double GRADE = 1.0; // Grade value for any manual shape

        switch (shape) {
            case CLUTTER:
                return new ClutterInter(null, GRADE);

            // Octave shift
            case OTTAVA:
            case QUINDICESIMA:
            case VENTIDUESIMA:
                return new OctaveShiftInter(shape, GRADE);

            // Brace, bracket
            case BRACE:
                return new BraceInter(GRADE);

            case BRACKET:
                return new BracketInter(GRADE);

            // Barlines
            case THIN_BARLINE:
            case THICK_BARLINE:
                return new StaffBarlineInter(shape, GRADE);

            case DOUBLE_BARLINE:
            case FINAL_BARLINE:
            case REVERSE_FINAL_BARLINE:
            case LEFT_REPEAT_SIGN:
            case RIGHT_REPEAT_SIGN:
            case BACK_TO_BACK_REPEAT_SIGN:
                return new StaffBarlineInter(shape, GRADE);

            // Beams
            case BEAM:
                return new BeamInter(GRADE);

            case BEAM_SMALL:
                return new SmallBeamInter(GRADE);

            case BEAM_HOOK:
                return new BeamHookInter(GRADE);

            // Ledger
            case LEDGER:
                return new LedgerInter(null, GRADE);

            // Stem
            case STEM:
                return new StemInter(null, GRADE);

            // Repeats
            case REPEAT_DOT:
                return new RepeatDotInter(null, GRADE, null, null); // No visit

            // Measure repeat signs
            case REPEAT_ONE_BAR:
            case REPEAT_TWO_BARS:
            case REPEAT_FOUR_BARS:
                return new MeasureRepeatInter(shape, GRADE);

            // Curves
            case SLUR_ABOVE:
                return new SlurInter(true, GRADE);

            case SLUR_BELOW:
                return new SlurInter(false, GRADE);

            case ENDING:
                return new EndingInter(false, GRADE);

            case ENDING_WRL:
                return new EndingInter(true, GRADE);

            // Text
            case LYRICS:
                return new LyricItemInter(GRADE);

            case TEXT:
                return new WordInter(shape, GRADE);

            case METRONOME:
                return new MetronomeInter(GRADE);

            // Clefs
            case G_CLEF:
            case G_CLEF_SMALL:
            case G_CLEF_8VA:
            case G_CLEF_8VB:
            case F_CLEF:
            case F_CLEF_SMALL:
            case F_CLEF_8VA:
            case F_CLEF_8VB:
            case C_CLEF:
            case PERCUSSION_CLEF:
                return new ClefInter(shape, GRADE);

            // Key signatures
            case KEY_FLAT_7:
            case KEY_FLAT_6:
            case KEY_FLAT_5:
            case KEY_FLAT_4:
            case KEY_FLAT_3:
            case KEY_FLAT_2:
            case KEY_FLAT_1:
            case KEY_CANCEL:
            case KEY_SHARP_1:
            case KEY_SHARP_2:
            case KEY_SHARP_3:
            case KEY_SHARP_4:
            case KEY_SHARP_5:
            case KEY_SHARP_6:
            case KEY_SHARP_7:
                return new KeyInter(GRADE, shape);

            // Custom / predefined number
            case NUMBER_CUSTOM:
            case TIME_ONE:
            case TIME_TWO:
            case TIME_THREE:
            case TIME_FOUR:
            case TIME_FIVE:
            case TIME_SIX:
            case TIME_SEVEN:
            case TIME_EIGHT:
            case TIME_NINE:
            case TIME_TWELVE:
            case TIME_SIXTEEN:
                return new NumberInter(null, shape, GRADE);

            // Predefined whole time
            case COMMON_TIME:
            case CUT_TIME:
            case TIME_FOUR_FOUR:
            case TIME_TWO_TWO:
            case TIME_TWO_FOUR:
            case TIME_THREE_FOUR:
            case TIME_FIVE_FOUR:
            case TIME_SIX_FOUR:
            case TIME_THREE_EIGHT:
            case TIME_SIX_EIGHT:
            case TIME_TWELVE_EIGHT:
                return new TimeWholeInter(null, shape, GRADE);

            case TIME_CUSTOM:
                return new TimeCustomInter(0, 0, GRADE);

            // Noteheads
            case BREVE:
            case WHOLE_NOTE:
            case NOTEHEAD_VOID:
            case NOTEHEAD_BLACK:

            case BREVE_SMALL:
            case WHOLE_NOTE_SMALL:
            case NOTEHEAD_VOID_SMALL:
            case NOTEHEAD_BLACK_SMALL:

            case BREVE_CROSS:
            case WHOLE_NOTE_CROSS:
            case NOTEHEAD_CROSS_VOID:
            case NOTEHEAD_CROSS:

            case BREVE_DIAMOND:
            case WHOLE_NOTE_DIAMOND:
            case NOTEHEAD_DIAMOND_VOID:
            case NOTEHEAD_DIAMOND_FILLED:

            case BREVE_TRIANGLE_DOWN:
            case WHOLE_NOTE_TRIANGLE_DOWN:
            case NOTEHEAD_TRIANGLE_DOWN_VOID:
            case NOTEHEAD_TRIANGLE_DOWN_FILLED:

            case BREVE_CIRCLE_X:
            case WHOLE_NOTE_CIRCLE_X:
            case NOTEHEAD_CIRCLE_X_VOID:
            case NOTEHEAD_CIRCLE_X:
                return new HeadInter(null, shape, GRADE, null, null);

            case AUGMENTATION_DOT:
                return new AugmentationDotInter(null, GRADE); // No visit

            // Compound notes
            case QUARTER_NOTE_UP:
            case QUARTER_NOTE_DOWN:
            case HALF_NOTE_UP:
            case HALF_NOTE_DOWN:
                return new CompoundNoteInter(null, null, shape, GRADE);

            // Flags
            case FLAG_1:
            case FLAG_2:
            case FLAG_3:
            case FLAG_4:
            case FLAG_5:
            case FLAG_1_DOWN:
            case FLAG_2_DOWN:
            case FLAG_3_DOWN:
            case FLAG_4_DOWN:
            case FLAG_5_DOWN:
                return new FlagInter(null, shape, GRADE);

            case SMALL_FLAG:
            case SMALL_FLAG_DOWN:
            case SMALL_FLAG_SLASH:
            case SMALL_FLAG_SLASH_DOWN:
                return new SmallFlagInter(null, shape, GRADE);

            // Rests
            case MULTIPLE_REST:
                return new MultipleRestInter(GRADE);

            case LONG_REST:
            case BREVE_REST:
            case WHOLE_REST:
            case HALF_REST:
            case QUARTER_REST:
            case EIGHTH_REST:
            case ONE_16TH_REST:
            case ONE_32ND_REST:
            case ONE_64TH_REST:
            case ONE_128TH_REST:
                return new RestInter(null, shape, GRADE, null, null); // No visit

            // Tuplets
            case TUPLET_THREE:
            case TUPLET_SIX:
                return new TupletInter(null, shape, GRADE); // No visit

            // Accidentals
            case FLAT:
            case NATURAL:
            case SHARP:
            case DOUBLE_SHARP:
            case DOUBLE_FLAT:
                return new AlterInter(null, shape, GRADE, null, null, null); // No visit

            // Articulations
            case ACCENT:
            case TENUTO:
            case STACCATO:
            case STACCATISSIMO:
            case STACCATISSIMO_BELOW:
            case MARCATO:
            case MARCATO_BELOW:
                return new ArticulationInter(null, shape, GRADE); // No visit

            // Markers
            case CODA:
            case SEGNO:
            case DAL_SEGNO:
            case DA_CAPO:
                return new MarkerInter(null, shape, GRADE); // No visit

            // Holds
            case FERMATA:
            case FERMATA_BELOW:
                return new FermataInter(null, shape, GRADE); // No visit

            // Pauses
            case BREATH_MARK:
                return new BreathMarkInter(null, GRADE); // No visit

            case CAESURA:
                return new CaesuraInter(null, GRADE); // No visit

            // Dynamics
            case DYNAMICS_P:
            case DYNAMICS_PP:
            case DYNAMICS_PPP:
            case DYNAMICS_MP:
            case DYNAMICS_F:
            case DYNAMICS_FF:
            case DYNAMICS_FFF:
            case DYNAMICS_MF:
            case DYNAMICS_FP:
            case DYNAMICS_FZ:
            case DYNAMICS_SF:
            case DYNAMICS_SFZ:
                return new DynamicsInter(null, shape, GRADE); // No visit

            // Wedges
            case CRESCENDO:
            case DIMINUENDO:
                return new WedgeInter(null, shape, GRADE);

            // Graces
            case GRACE_NOTE:
            case GRACE_NOTE_DOWN:
            case GRACE_NOTE_SLASH:
            case GRACE_NOTE_SLASH_DOWN:
                return new GraceChordInter(null, shape, GRADE);

            // Metronome units
            case METRO_WHOLE:
            case METRO_HALF:
            case METRO_QUARTER:
            case METRO_EIGHTH:
            case METRO_SIXTEENTH:
            case METRO_DOTTED_HALF:
            case METRO_DOTTED_QUARTER:
            case METRO_DOTTED_EIGHTH:
            case METRO_DOTTED_SIXTEENTH:
                return new BeatUnitInter(shape, GRADE);

            // Ornaments
            case TR:
            case TURN:
            case TURN_INVERTED:
            case TURN_UP:
            case TURN_SLASH:
            case MORDENT:
            case MORDENT_INVERTED:
                return new OrnamentInter(null, shape, GRADE); // No visit

            // Tremolos
            case TREMOLO_1:
            case TREMOLO_2:
            case TREMOLO_3:
                return new TremoloInter(shape, GRADE);

            // Plucked techniques
            case ARPEGGIATO:
                return new ArpeggiatoInter(null, GRADE);

            // Strings techniques
            case BOW_DOWN:
            case BOW_UP:
                return new BowInter(null, shape, GRADE); // No visit

            // Keyboards
            case PEDAL_MARK:
            case PEDAL_UP_MARK:
                return new PedalInter(null, shape, GRADE); // No visit

            // Fingering
            case DIGIT_0:
            case DIGIT_1:
            case DIGIT_2:
            case DIGIT_3:
            case DIGIT_4:
            case DIGIT_5:
                return new FingeringInter(null, shape, GRADE); // No visit

            // Plucking
            case PLUCK_P:
            case PLUCK_I:
            case PLUCK_M:
            case PLUCK_A:
                return new PluckingInter(null, shape, GRADE); // No visit

            // Romans
            case ROMAN_I:
            case ROMAN_II:
            case ROMAN_III:
            case ROMAN_IV:
            case ROMAN_V:
            case ROMAN_VI:
            case ROMAN_VII:
            case ROMAN_VIII:
            case ROMAN_IX:
            case ROMAN_X:
            case ROMAN_XI:
            case ROMAN_XII:
                return new FretInter(null, shape, GRADE); // No visit

            // Playings
            case PLAYING_OPEN:
            case PLAYING_HALF_OPEN:
            case PLAYING_CLOSED:
                return new PlayingInter(null, shape, GRADE);

            // Others
            default:
                logger.warn("No ghost instance for {}", shape);

                return null;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean protectMidSystemClef = new Constant.Boolean(
                true,
                "Should a mid-system clef claim its own ink and be kept above the weak floor?");

        private final Evaluation.Grade minClefGrade = new Evaluation.Grade(
                0.35,
                "Minimum classifier grade for a mid-system clef to be protected");

        private final Evaluation.Grade minReturningClefGrade = new Evaluation.Grade(
                0.20,
                "Minimum classifier grade for a clef that returns a staff to its header clef");

        private final Constant.Ratio minClefHeight = new Constant.Ratio(
                2.0,
                "Minimum clef glyph height, in interlines, to claim heads");

        private final Constant.Ratio maxClefHeight = new Constant.Ratio(
                8.0,
                "Maximum clef glyph height, in interlines, to claim heads");

        private final Constant.Ratio minClefWidth = new Constant.Ratio(
                1.0,
                "Minimum clef glyph width, in interlines, to claim heads");

        private final Constant.Ratio maxClefWidth = new Constant.Ratio(
                4.0,
                "Maximum clef glyph width, in interlines, to claim heads");

        private final Constant.Ratio maxLinePitchOffset = new Constant.Ratio(
                0.5,
                "Maximum pitch distance from a staff line for a C clef center");
    }
}

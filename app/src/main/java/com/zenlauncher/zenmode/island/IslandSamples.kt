package com.zenlauncher.zenmode.island

import com.zenlauncher.zenmode.coreapi.week.MomentCopy
import com.zenlauncher.zenmode.widget.WidgetData
import com.zenlauncher.zenmode.widget.glance.WidgetDemo

/** The island with a sample week in it: Settings' preview strip, and the screenshot tests. */
object IslandSamples {

    /** The week's moment as the island shows it, from a snapshot (the demo week by default). */
    fun moment(d: WidgetData = WidgetDemo.data(), reason: MomentPlan.Reason = MomentPlan.Reason.DECISIVE_DAY): MomentItem =
        MomentItem(
            moment = d.moment,
            words = MomentCopy.line(d.moment, MomentCopy.daySeed(d.today)),
            eyebrow = MomentCopy.eyebrow(d.moment),
            scoreTenths = d.todayScore,
            reason = reason,
            endsAt = Long.MAX_VALUE
        )

    /** Zeno and the week in the pill. */
    val preview: IslandScene by lazy { IslandScene(main = moment()) }
}

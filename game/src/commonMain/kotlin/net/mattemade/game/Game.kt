package net.mattemade.game

import com.littlekt.Context
import com.littlekt.ContextListener
import net.mattemade.utils.releasing.Releasing
import net.mattemade.utils.releasing.Self

class Game(
    context: Context,
) : ContextListener(context),
    Releasing by Self() {

    override suspend fun Context.start() {

        onRender { dt ->

        }

        onDispose(::release)
    }

}

package com.ruuvi.station.util.extensions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.kodein.di.KodeinAware
import org.kodein.di.direct
import org.kodein.di.generic.instance

/**
 * Example usage:
 *
 * Define your kodein binding as PROVIDER (since there is no argument):
 * ```kotlin
 *     bind<SomeViewModel>() with provider {
 *         SomeViewModel(instance(),... )
 *     }
 * ```
 * And then inject your view model in following manner:
 * ```kotlin
 *     class SomeFragment: Fragment(), KodeinAware {
 *         val viewModel: SomeViewModel by viewModel()
 *     }
 * ```
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified TViewModel, TFragment> TFragment.viewModel(
    tag: String? = null
): Lazy<TViewModel>
    where TViewModel : ViewModel,
          TFragment : KodeinAware,
          TFragment : Fragment {

    return lazy {
        ViewModelProviders
            .of(this, object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>) =
                    kodein.direct.instance<TViewModel>(tag) as T
            })
            .get(TViewModel::class.java)
    }
}

/**
 * Example usage:
 *
 * Define your kodein binding as PROVIDER (since there is no argument):
 * ```kotlin
 *     bind<SomeViewModel>() with provider {
 *         SomeViewModel(instance(),... )
 *     }
 * ```
 * And then inject your view model in following manner:
 * ```kotlin
 *     class SomeActivity: FragmentActivity(), KodeinAware {
 *         val viewModel: SomeViewModel by viewModel()
 *     }
 * ```
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified TViewModel, TActivity> TActivity.viewModel(
    tag: String? = null
): Lazy<TViewModel>
    where TViewModel : ViewModel,
          TActivity : KodeinAware,
          TActivity : FragmentActivity {

    return lazy {
        ViewModelProviders
            .of(this, object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>) =
                    kodein.direct.instance<TViewModel>(tag) as T
            })
            .get(TViewModel::class.java)
    }
}

/**
 * Example usage:
 *
 * Define your kodein binding as PROVIDER (since there is no argument):
 * ```kotlin
 *     bind<SomeViewModel>() with provider {
 *         SomeViewModel(instance(),... )
 *     }
 * ```
 * And then inject your view model in following manner:
 * ```kotlin
 *     class SomeFragment: Fragment(), KodeinAware {
 *         val viewModel: SomeViewModel by sharedViewModel()
 *     }
 * ```
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified TViewModel, TFragment> TFragment.sharedViewModel(
    tag: String? = null
): Lazy<TViewModel>
    where TViewModel : ViewModel,
          TFragment : KodeinAware,
          TFragment : Fragment {

    return lazy {
        ViewModelProviders
            .of(requireActivity(), object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>) =
                    kodein.direct.instance<TViewModel>(tag) as T
            })
            .get(TViewModel::class.java)
    }
}

/**
 * Example usage:
 *
 * Define your view models initialization argument as a data class (due to kodein limitation of args can be passed):
 * ```kotlin
 *     data class ViewModelArgument(val a: Int, val b: Int)
 * ```
 * Define your kodein binding as FACTORY (there are some arguments, remember?):
 * ```kotlin
 *     bind<SomeViewModel>() with factory { ar
 *         SomeViewModel(argument, instance(),... )
 *     }
 * ```
 * And then inject your view model in following manner:
 * ```kotlin
 *     class SomeFragment: Fragment(), KodeinAware {
 *         val viewModel: SomeViewModel by viewModel { createOrGetArgument() }
 *     }
 * ```
 * Getting by lambda allows lazy initialization so it is possible to use i.e. fragment arguments or activity extras.
 *
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified TViewModel, reified TArgument, TFragment> TFragment.viewModel(
    tag: String? = null,
    crossinline argFactory: () -> TArgument
): Lazy<TViewModel>
    where TViewModel : ViewModel,
          TFragment : KodeinAware,
          TFragment : Fragment {

    return lazy {
        ViewModelProviders
            .of(this, object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>) =
                    kodein.direct.instance<TArgument, TViewModel>(tag, argFactory()) as T
            })
            .get(TViewModel::class.java)
    }
}

/**
 * Example usage:
 *
 * Define your view models initialization argument as a data class (due to kodein limitation of args can be passed):
 * ```kotlin
 *     data class ViewModelArgument(val a: Int, val b: Int)
 * ```
 * Define your kodein binding as FACTORY (there are some arguments, remember?):
 * ```kotlin
 *     bind<SomeViewModel>() with factory { argument: ViewModelArgument ->
 *         SomeViewModel(argument, instance(),... )
 *     }
 * ```
 * And then inject your view model in following manner:
 * ```kotlin
 *     class SomeActivity: FragmentActivity(), KodeinAware {
 *         val viewModel: SomeViewModel by viewModel { createOrGetArgument() }
 *     }
 * ```
 * Getting by lambda allows lazy initialization so it is possible to use i.e. fragment arguments or activity extras.
 *
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified TViewModel, reified TArgument, TActivity> TActivity.viewModel(
    tag: String? = null,
    crossinline argFactory: () -> TArgument
): Lazy<TViewModel>
    where TViewModel : ViewModel,
          TActivity : KodeinAware,
          TActivity : FragmentActivity {
    return lazy {
        ViewModelProviders
            .of(this, object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>) =
                    kodein.direct.instance<TArgument, TViewModel>(tag, argFactory()) as T
            })
            .get(TViewModel::class.java)
    }
}

/**
 * Example usage:
 *
 * Define your view models initialization argument as a data class (due to kodein limitation of args can be passed):
 * ```kotlin
 *     data class ViewModelArgument(val a: Int, val b: Int)
 * ```
 * Define your kodein binding as FACTORY (there are some arguments, remember?):
 * ```kotlin
 *     bind<SomeViewModel>() with factory { argument: ViewModelArgument ->
 *         SomeViewModel(argument, instance(),... )
 *     }
 * ```
 * And then inject your view model in following manner:
 * ```kotlin
 *     class SomeFragment: Fragment(), KodeinAware {
 *         val viewModel: SomeViewModel by sharedViewModel { createOrGetArgument() }
 *     }
 * ```
 * Getting by lambda allows lazy initialization so it is possible to use i.e. fragment arguments or activity extras.
 *
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified TViewModel, reified TArgument, TFragment> TFragment.sharedViewModel(
    tag: String? = null,
    crossinline argFactory: () -> TArgument
): Lazy<TViewModel>
    where TViewModel : ViewModel,
          TFragment : KodeinAware,
          TFragment : Fragment {

    return lazy {
        ViewModelProviders
            .of(requireActivity(), object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(aClass: Class<T>) =
                    kodein.direct.instance<TArgument, TViewModel>(tag, argFactory()) as T
            })
            .get(TViewModel::class.java)
    }
}

/**
 * Example usage:
 *
 * Define your view models initialization argument as any Fragment argument type class, i.e. Parcelable
 *
 *  * ```kotlin
 *     @Parcelize
 *     class Post: Parcelable {
 *        var title:String? = null
 *     }
 * ```
 *
 * ```kotlin
 *     class SomeFragment: Fragment() {
 *         val post: Post by arg("ARG_POST)
 *     }
 * ```
 * Getting by lambda allows lazy initialization so it is possible to use i.e. fragment arguments or activity extras.
 *
 */
inline fun <reified TArg, TFragment> TFragment.arg(
    argName: String
): Lazy<TArg>
    where TFragment : Fragment {
    return lazy {
        val argument = arguments?.get(argName)
        if (argument != null) {
            return@lazy argument as TArg
        } else {
            throw IllegalArgumentException("Could not get argument $argName")
        }
    }
}
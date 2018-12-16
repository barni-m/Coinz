package uk.ac.ed.inf.coinz;


import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WalletFragmentTest {

    @Rule
    public ActivityTestRule<MapActivity> mActivityTestRule = new ActivityTestRule<>(MapActivity.class);

    @Test
    public void walletFragmentTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.email_input_field),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.email_input_field),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("usertest2@coin"), closeSoftKeyboard());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.email_input_field), withText("usertest2@coin"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText3.perform(replaceText("usertest2@coinz.com"));

        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(R.id.email_input_field), withText("usertest2@coinz.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText4.perform(closeSoftKeyboard());

        ViewInteraction appCompatEditText5 = onView(
                allOf(withId(R.id.password_input_field),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatEditText5.perform(replaceText("pass123"), closeSoftKeyboard());

        ViewInteraction appCompatEditText6 = onView(
                allOf(withId(R.id.password_input_field), withText("pass123"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatEditText6.perform(pressImeActionButton());

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.login_signup_button), withText("Login"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatButton.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(8100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.menu_button),
                        childAtPosition(
                                allOf(withId(R.id.menu_button_container),
                                        childAtPosition(
                                                withId(R.id.mapMainLayout),
                                                2)),
                                0),
                        isDisplayed()));
        appCompatButton2.perform(click());


        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatImageView = onView(
                allOf(withId(R.id.coin_dolr),
                        childAtPosition(
                                allOf(withId(R.id.constraint_layout_wallet),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatImageView.perform(click());

        ViewInteraction textView = onView(
                allOf(withId(R.id.coin_currency_name), withText("DOLR"),
                        childAtPosition(
                                allOf(withId(R.id.constraint_layout_wallet),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                7),
                        isDisplayed()));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        ViewInteraction appCompatImageView2 = onView(
                allOf(withId(R.id.coin_quid),
                        childAtPosition(
                                allOf(withId(R.id.constraint_layout_wallet),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                5),
                        isDisplayed()));
        appCompatImageView2.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.coin_currency_name), withText("QUID"),
                        childAtPosition(
                                allOf(withId(R.id.constraint_layout_wallet),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                7),
                        isDisplayed()));
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        ViewInteraction appCompatImageView3 = onView(
                allOf(withId(R.id.coin_shil),
                        childAtPosition(
                                allOf(withId(R.id.constraint_layout_wallet),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatImageView3.perform(click());

        ViewInteraction appCompatImageView4 = onView(
                allOf(withId(R.id.coin_peny),
                        childAtPosition(
                                allOf(withId(R.id.constraint_layout_wallet),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatImageView4.perform(click());

        ViewInteraction bottomNavigationItemView = onView(
                allOf(withId(R.id.nav_bottom_bank),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_navigation),
                                        0),
                                1),
                        isDisplayed()));
        bottomNavigationItemView.perform(click());

        try {
            Thread.sleep(7900);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction bottomNavigationItemView2 = onView(
                allOf(withId(R.id.nav_bottom_wallet),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_navigation),
                                        0),
                                0),
                        isDisplayed()));
        bottomNavigationItemView2.perform(click());

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.coin_currency_name), withText("PENY"),
                        childAtPosition(
                                allOf(withId(R.id.constraint_layout_wallet),
                                        childAtPosition(
                                                withId(R.id.fragment_container),
                                                0)),
                                7),
                        isDisplayed()));
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}

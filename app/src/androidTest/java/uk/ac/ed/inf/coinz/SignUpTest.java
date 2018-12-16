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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;



@LargeTest
@RunWith(AndroidJUnit4.class)
public class SignUpTest {

    @Rule
    public ActivityTestRule<MapActivity> mActivityTestRule = new ActivityTestRule<>(MapActivity.class);

    @Test
    public void signUpTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html


        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        if(mActivityTestRule.getActivity().getLoggedIn()){
            new LogOutTest().logOutTest();
            new ResetFirebase().resetFirebase();
        }


        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.sign_up_link), withText("Sign up here!"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                8),
                        isDisplayed()));
        appCompatButton.perform(click());

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

        ViewInteraction appCompatEditText2 = onView(
                allOf(withId(R.id.email_input_field),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText2.perform(replaceText("usertest@co"), closeSoftKeyboard());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText3 = onView(
                allOf(withId(R.id.email_input_field), withText("usertest@co"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText3.perform(replaceText("usertest@coinz.com"));

        ViewInteraction appCompatEditText4 = onView(
                allOf(withId(R.id.email_input_field), withText("usertest@coinz.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText4.perform(closeSoftKeyboard());

        ViewInteraction appCompatEditText5 = onView(
                allOf(withId(R.id.email_input_field), withText("usertest@coinz.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText5.perform(pressImeActionButton());

        ViewInteraction appCompatEditText6 = onView(
                allOf(withId(R.id.emai_confirm_input_field),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText6.perform(replaceText("usertest"), closeSoftKeyboard());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText7 = onView(
                allOf(withId(R.id.emai_confirm_input_field), withText("usertest"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText7.perform(replaceText("usertest@coins.com"));

        ViewInteraction appCompatEditText8 = onView(
                allOf(withId(R.id.emai_confirm_input_field), withText("usertest@coins.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText8.perform(closeSoftKeyboard());

        ViewInteraction appCompatEditText9 = onView(
                allOf(withId(R.id.emai_confirm_input_field), withText("usertest@coins.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText9.perform(pressImeActionButton());

        ViewInteraction appCompatEditText10 = onView(
                allOf(withId(R.id.password_input_field),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatEditText10.perform(replaceText("pass123"), closeSoftKeyboard());

        ViewInteraction appCompatEditText11 = onView(
                allOf(withId(R.id.password_input_field), withText("pass123"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatEditText11.perform(pressImeActionButton());

        ViewInteraction appCompatEditText12 = onView(
                allOf(withId(R.id.password_confirm_input_field),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                5),
                        isDisplayed()));
        appCompatEditText12.perform(replaceText("pass12"), closeSoftKeyboard());


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.login_signup_button), withText("Sign up"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatButton2.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatEditText13 = onView(
                allOf(withId(R.id.password_confirm_input_field), withText("pass12"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                5),
                        isDisplayed()));
        appCompatEditText13.perform(replaceText("pass123"));

        ViewInteraction appCompatEditText14 = onView(
                allOf(withId(R.id.password_confirm_input_field), withText("pass123"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                5),
                        isDisplayed()));
        appCompatEditText14.perform(closeSoftKeyboard());

        ViewInteraction appCompatEditText15 = onView(
                allOf(withId(R.id.password_confirm_input_field), withText("pass123"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                5),
                        isDisplayed()));
        appCompatEditText15.perform(pressImeActionButton());

        ViewInteraction appCompatButton3 = onView(
                allOf(withId(R.id.login_signup_button), withText("Sign up"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatButton3.perform(click());

        ViewInteraction appCompatEditText16 = onView(
                allOf(withId(R.id.emai_confirm_input_field), withText("usertest@coins.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText16.perform(replaceText("usertest@coin.com"));

        ViewInteraction appCompatEditText17 = onView(
                allOf(withId(R.id.emai_confirm_input_field), withText("usertest@coin.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText17.perform(closeSoftKeyboard());


        ViewInteraction appCompatEditText18 = onView(
                allOf(withId(R.id.emai_confirm_input_field), withText("usertest@coin.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText18.perform(replaceText("usertest@coinz.com"));

        ViewInteraction appCompatEditText19 = onView(
                allOf(withId(R.id.emai_confirm_input_field), withText("usertest@coinz.com"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText19.perform(closeSoftKeyboard());


        ViewInteraction appCompatButton4 = onView(
                allOf(withId(R.id.login_signup_button), withText("Sign up"),
                        childAtPosition(
                                allOf(withId(R.id.RelativeLayout),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatButton4.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        boolean loggedIn = mActivityTestRule.getActivity().getLoggedIn();
        Assert.assertTrue("Logged in is:", loggedIn);

        try {
            Thread.sleep(4501);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new LogOutTest().logOutTest();
        new LoginTest().loginTest();

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

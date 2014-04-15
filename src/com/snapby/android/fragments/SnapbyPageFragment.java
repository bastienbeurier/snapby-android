package com.snapby.android.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.snapby.android.R;
import com.snapby.android.activities.CommentsActivity;
import com.snapby.android.activities.DisplayActivity;
import com.snapby.android.activities.MainActivity;
import com.snapby.android.models.Shout;
import com.snapby.android.utils.ApiUtils;
import com.snapby.android.utils.Constants;
import com.snapby.android.utils.GeneralUtils;
import org.json.JSONObject;

/**
 * Created by bastien on 3/3/14.
 */
public class SnapbyPageFragment extends Fragment {

    private Shout shout = null;

    private AQuery aq = null;

    private ImageView imageView = null;

    private TextView usernameView = null;

    private ImageView userProfilePic = null;

    private LinearLayout userContainer = null;

    private View snapbyInfoContainer = null;

    private View likeContainer = null;

    private ImageView likeIcon = null;

    private TextView likeCount = null;

    private View commentContainer = null;

    private ImageView commentIcon = null;

    private TextView commentCount = null;

    private View imageContainer = null;

    private boolean liked = false;

    private MainActivity mainActivity = null;

    private String type = null;

    public static SnapbyPageFragment newInstance(Shout shout) {
        SnapbyPageFragment snapbyPageFragment = new SnapbyPageFragment();

        Bundle args = new Bundle();
        args.putParcelable("shout", shout);
        snapbyPageFragment.setArguments(args);

        return snapbyPageFragment;
    }

    public static SnapbyPageFragment newInstance(Shout shout, String type) {
        SnapbyPageFragment snapbyPageFragment = new SnapbyPageFragment();

        Bundle args = new Bundle();
        args.putParcelable("shout", shout);
        args.putString("type", type);
        snapbyPageFragment.setArguments(args);

        return snapbyPageFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        aq = GeneralUtils.getAquery(mainActivity);

        if ((getArguments().getString("type") != null && getArguments().getString("type").equals("profile"))) {
            type = "profile";
        } else {
            type = "explore";
        }

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.shout_page_fragment, container, false);
        shout = getArguments().getParcelable("shout");

        imageView = (ImageView) rootView.findViewById(R.id.explore_shout_image);
        usernameView = (TextView) rootView.findViewById(R.id.explore_shout_username);
        userProfilePic = (ImageView) rootView.findViewById(R.id.explore_shout_user_picture);
        userContainer = (LinearLayout) rootView.findViewById(R.id.explore_shout_user_container);
        imageContainer = rootView.findViewById(R.id.shout_image_view_container);
        snapbyInfoContainer = rootView.findViewById(R.id.explore_shout_info_container);
        likeContainer = rootView.findViewById(R.id.explore_shout_like_container);
        likeIcon = (ImageView) rootView.findViewById(R.id.explore_shout_like_button);
        likeCount = (TextView) rootView.findViewById(R.id.explore_shout_like_count);
        commentContainer = rootView.findViewById(R.id.explore_shout_comment_container);
        commentIcon = (ImageView) rootView.findViewById(R.id.explore_shout_comment_button);
        commentCount = (TextView) rootView.findViewById(R.id.explore_shout_comment_count);

        if (Constants.PRODUCTION) {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_PROD + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        } else {
            aq.id(imageView).image(Constants.SMALL_SHOUT_IMAGE_URL_PREFIX_DEV + shout.id + "--400", true, false, 0, 0, null, AQuery.FADE_IN);
        }

        if (shout.anonymous || type.equals("profile")) {
            userContainer.setVisibility(View.GONE);
        } else {
            userContainer.setVisibility(View.VISIBLE);
            imageContainer.setVisibility(View.VISIBLE);
            aq.id(userProfilePic).image(GeneralUtils.getProfileThumbPicturePrefix() + shout.userId, true, false, 0, 0, null, AQuery.FADE_IN);

            userContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Intent profile = new Intent(getActivity(), ProfileActivity.class);
//                    startActivityForResult(profile, Constants.PROFILE_REQUEST);
                }
            });
        }

        if (shout.anonymous) {
            usernameView.setText(getResources().getString(R.string.anonymous_name));
            usernameView.setTextColor(getResources().getColor(R.color.anonymousGrey));
        } else {
            usernameView.setText("@" + shout.username);
        }

        setLikeCountUI(shout.likeCount);
        setCommentCountUI(shout.commentCount);

        if (mainActivity.myLikes.contains(shout.id)) {
            liked = true;
            likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button_liked));
        } else {
            liked = false;
            likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button));
        }

        likeContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snapbyInfoContainer.setEnabled(false);

                if (liked) {
                    unlikeShout();

                    ApiUtils.removeLike(mainActivity, shout.id, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            snapbyInfoContainer.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(mainActivity, getString(R.string.shout_unlike_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                likeShout();
                            }
                        }
                    });
                } else {
                    likeShout();

                    ApiUtils.createLike(mainActivity, shout, 0, 0, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            super.callback(url, object, status);

                            snapbyInfoContainer.setEnabled(true);

                            if (status.getError() != null) {
                                Toast toast = Toast.makeText(mainActivity, getString(R.string.shout_like_failed), Toast.LENGTH_SHORT);
                                toast.show();

                                unlikeShout();
                            }
                        }
                    });
                }
            }
        });

        commentContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent comments = new Intent(getActivity(), CommentsActivity.class);
                comments.putExtra("shout", shout);

                startActivityForResult(comments, Constants.COMMENTS_REQUEST);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent displayShout = new Intent(getActivity(), DisplayActivity.class);
                displayShout.putExtra("shout", shout);

                startActivityForResult(displayShout, Constants.DISPLAY_SHOUT_REQUEST);
            }
        });

        return rootView;
    }

    private void likeShout() {
        liked = true;
        likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button_liked));
        mainActivity.myLikes.add(shout.id);

        if (type.equals("profile")) {
            mainActivity.reloadExploreShouts();
        } else {
            mainActivity.reloadProfileShouts();
        }

        shout.likeCount++;
        setLikeCountUI(shout.likeCount);
    }

    private void unlikeShout() {
        liked = false;
        likeIcon.setImageDrawable(getResources().getDrawable(R.drawable.explore_shout_like_button));
        mainActivity.myLikes.remove(shout.id);

        if (type.equals("profile")) {
            mainActivity.reloadExploreShouts();
        } else {
            mainActivity.reloadProfileShouts();
        }

        shout.likeCount--;
        setLikeCountUI(shout.likeCount);
    }

    private void setLikeCountUI(int count) {
        if (count > 1) {
            likeCount.setText(Integer.toString(count));
        } else {
            likeCount.setText(Integer.toString(count));
        }
    }

    private void setCommentCountUI(int count) {
        if (count > 1) {
            commentCount.setText(Integer.toString(count));
        } else {
            commentCount.setText(Integer.toString(count));
        }
    }
}
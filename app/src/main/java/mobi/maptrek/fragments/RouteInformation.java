/*
 * Copyright 2019 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import mobi.maptrek.MapHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Route;
import mobi.maptrek.util.StringFormatter;

public class RouteInformation extends ListFragment implements PopupMenu.OnMenuItemClickListener {
    private Route mRoute;

    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;
    private OnRouteActionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_with_empty_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InstructionListAdapter adapter = new InstructionListAdapter();
        setListAdapter(adapter);

        ListView listView = getListView();
        View headerView = LayoutInflater.from(view.getContext()).inflate(R.layout.list_header_route_title, listView, false);

        ImageButton moreButton = headerView.findViewById(R.id.moreButton);
        moreButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(getContext(), moreButton);
            moreButton.setOnTouchListener(popup.getDragToOpenListener());
            popup.inflate(R.menu.context_menu_route);
            popup.setOnMenuItemClickListener(RouteInformation.this);
            popup.show();
        });

        listView.addHeaderView(headerView, null, false);

        initializeRouteInformation();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnRouteActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement OnRouteActionListener");
        }
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement MapHolder");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement FragmentHolder");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mFragmentHolder.disableListActionButton();
        mFragmentHolder = null;
        mMapHolder = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mFloatingButton = mFragmentHolder.enableListActionButton();
        mFloatingButton.setImageResource(R.drawable.ic_navigate);
        mFloatingButton.setOnClickListener(v -> {
            mMapHolder.navigateVia(mRoute);
            mFragmentHolder.popAll();
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mFragmentHolder.disableListActionButton();
        mFloatingButton = null;
    }

    @Override
    public void onListItemClick(@NonNull ListView lv, @NonNull View v, int position, long id) {
        mMapHolder.setMapLocation(mRoute.get(position));
        mFragmentHolder.popAll();
    }

    public void setRoute(Route route) {
        mRoute = route;
        if (isVisible()) {
            initializeRouteInformation();
        }
    }

    private void initializeRouteInformation() {
        View rootView = getView();
        assert rootView != null;

        ((TextView) rootView.findViewById(R.id.name)).setText(mRoute.name);

        View sourceRow = rootView.findViewById(R.id.sourceRow);
        if (mRoute.source == null || mRoute.source.isNativeTrack()) { // TODO: isNativeTrack
            sourceRow.setVisibility(View.GONE);
        } else {
            ((TextView) rootView.findViewById(R.id.source)).setText(mRoute.source.name);
            sourceRow.setVisibility(View.VISIBLE);
        }

        String distance = StringFormatter.distanceHP(mRoute.distance);
        ((TextView) rootView.findViewById(R.id.distance)).setText(distance);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_navigate_reversed) {
            mMapHolder.navigateViaReversed(mRoute);
            mFragmentHolder.popAll();
            return true;
        }
        if (itemId == R.id.action_share) {
            mListener.onRouteShare(mRoute);
            return true;
        }
        return false;
    }

    private class InstructionListAdapter extends BaseAdapter {

        @Override
        public Route.Instruction getItem(int position) {
            return mRoute.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getCount() {
            return mRoute.size();
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            InstructionListItemHolder itemHolder;

            if (convertView == null) {
                itemHolder = new InstructionListItemHolder();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_route_instruction, parent, false);
                itemHolder.text = convertView.findViewById(R.id.text);
                itemHolder.distance = convertView.findViewById(R.id.distance);
                itemHolder.sign = convertView.findViewById(R.id.sign);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (InstructionListItemHolder) convertView.getTag();
            }

            itemHolder.text.setText(mRoute.getInstructionText(position));
            double distance = mRoute.distanceBetween(position - 1, position);
            itemHolder.distance.setText(distance > 0 ? StringFormatter.distanceH(distance) : "");
            itemHolder.sign.setImageResource(getSignDrawable(mRoute.getSign(position)));

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    private static class InstructionListItemHolder {
        TextView text;
        TextView distance;
        ImageView sign;
    }

    @DrawableRes
    public static int getSignDrawable(int sign) {
        @DrawableRes int signDrawable; // https://thenounproject.com/dergraph/collection/travel-navigation/
        switch (sign) {
            case Route.Instruction.START:
                signDrawable = R.drawable.instruction_start;
                break;
            case Route.Instruction.U_TURN_UNKNOWN:
            case Route.Instruction.U_TURN_LEFT:
                signDrawable = R.drawable.instruction_u_turn_left;
                break;
            case Route.Instruction.KEEP_LEFT:
                signDrawable = R.drawable.instruction_keep_left;
                break;
            case Route.Instruction.TURN_SHARP_LEFT:
                signDrawable = R.drawable.instruction_turn_sharp_left;
                break;
            case Route.Instruction.TURN_LEFT:
                signDrawable = R.drawable.instruction_turn_left;
                break;
            case Route.Instruction.TURN_SLIGHT_LEFT:
                signDrawable = R.drawable.instruction_turn_slight_left;
                break;
            case Route.Instruction.TURN_SLIGHT_RIGHT:
                signDrawable = R.drawable.instruction_turn_slight_right;
                break;
            case Route.Instruction.TURN_RIGHT:
                signDrawable = R.drawable.instruction_turn_right;
                break;
            case Route.Instruction.TURN_SHARP_RIGHT:
                signDrawable = R.drawable.instruction_turn_sharp_right;
                break;
            case Route.Instruction.FINISH:
                signDrawable = R.drawable.instruction_finish;
                break;
            case Route.Instruction.REACHED_VIA:
                signDrawable = R.drawable.instruction_via_reached;
                break;
            case Route.Instruction.LEAVE_ROUNDABOUT: // TODO Make separate icon
            case Route.Instruction.USE_ROUNDABOUT:
                signDrawable = R.drawable.instruction_use_roundabout;
                break;
            case Route.Instruction.KEEP_RIGHT:
                signDrawable = R.drawable.instruction_keep_right;
                break;
            case Route.Instruction.U_TURN_RIGHT:
                signDrawable = R.drawable.instruction_u_turn_right;
                break;
            case Route.Instruction.CONTINUE_ON_STREET:
            case Route.Instruction.IGNORE:
            case Route.Instruction.UNKNOWN:
            default:
                signDrawable = R.drawable.instruction_continue_on_street;
                break;
        }
        return signDrawable;
    }
}

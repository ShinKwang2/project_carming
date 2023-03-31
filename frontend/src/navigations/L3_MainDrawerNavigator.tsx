import {createDrawerNavigator} from '@react-navigation/drawer';
import HomeScreen from '../screens/HomeScreen';
import UserPlacesScreen from '../screens/UserPlacesScreen';
import UserCoursesScreen from '../screens/UserCoursesScreen';
import UserPaymentsScreen from '../screens/UserPaymentsScreen';
import {MainHeaderTitleLogo} from '../components';

export type L3_MainDrawerParamList = {
  home: undefined;
  UserPlaces: undefined;
  UserCourses: undefined;
  UserPayments: undefined;
};

const Drawer = createDrawerNavigator<L3_MainDrawerParamList>();

function L3_MainDrawerNavigator() {
  return (
    <Drawer.Navigator
      screenOptions={{
        drawerPosition: 'right',
        headerStyle: {
          backgroundColor: '#8398D1',
        },
        headerTintColor: '#fff',
        headerTitleAlign: 'center',
        headerTitle: () => MainHeaderTitleLogo(),
      }}>
      <Drawer.Screen name="home" component={HomeScreen} />
      <Drawer.Screen name="UserPlaces" component={UserPlacesScreen} />
      <Drawer.Screen name="UserCourses" component={UserCoursesScreen} />
      <Drawer.Screen name="UserPayments" component={UserPaymentsScreen} />
    </Drawer.Navigator>
  );
}

export default L3_MainDrawerNavigator;

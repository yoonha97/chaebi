import './global.css';
import React from 'react';
import {NavigationContainer} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {ToastProvider} from './src/components/ToastContext';
import SplashScreen from './src/screens/Splash';
import SignInScreen from './src/screens/Signin';
import AppIntroScreen from './src/screens/Appintro';
import SignUpScreen from './src/screens/Signup';
import RemainScreen, {Recipient} from './src/screens/Remain';
import AbsenceScreen from './src/screens/Absence';
import SendCodeScreen from './src/screens/Sendcode';
import SetPasswordScreen from './src/screens/Setpw';
import ContactScreen from './src/screens/Remain/contacts';
import RemainWriteScreen from './src/screens/Remain/write';
import QuestionScreen from './src/screens/Remain/question';
import MainScreen from './src/screens/Main';
import RemainEditorScreen from './src/screens/RemainEditor';
import CompleteScreen from './src/screens/Remain/complete';
import AlbumScreen from './src/screens/Album';
import MypageScreen from './src/screens/Mypage';
import SetAlertScreen from './src/screens/Mypage/SetAlert';
import CheckPasswordScreen from './src/screens/CheckPw';

import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import SetLockScreen from './src/screens/Mypage/SetLock';
import {GestureHandlerRootView} from 'react-native-gesture-handler';

export type RootStackParamList = {
  Splash: undefined;
  Remain: undefined;
  AppIntro: undefined;
  SignUp: undefined;
  SignIn: undefined;
  Absence: undefined;
  SendCode: undefined;
  Contacts: undefined;
  RemainWrite: undefined;
  RemainQuestion: Recipient;
  RemainComplete: Recipient;
  Main: undefined;
  SetPw: undefined;
  RemainEditor: Recipient;
  Album: undefined;
  Setting: undefined;
  SetAlert: undefined;
  SetLock: undefined;
  CheckPw: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

const queryClient = new QueryClient();

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <GestureHandlerRootView>
          <NavigationContainer>
            <Stack.Navigator screenOptions={{headerShown: false}}>
              <Stack.Screen name="Splash" component={SplashScreen} />
              <Stack.Screen name="AppIntro" component={AppIntroScreen} />
              <Stack.Screen name="SignUp" component={SignUpScreen} />
              <Stack.Screen name="SignIn" component={SignInScreen} />
              <Stack.Screen name="Absence" component={AbsenceScreen} />
              <Stack.Screen name="SendCode" component={SendCodeScreen} />
              <Stack.Screen name="Remain" component={RemainScreen} />
              <Stack.Screen name="Contacts" component={ContactScreen} />
              <Stack.Screen name="RemainWrite" component={RemainWriteScreen} />
              <Stack.Screen name="RemainQuestion" component={QuestionScreen} />
              <Stack.Screen name="RemainComplete" component={CompleteScreen} />
              <Stack.Screen name="Main" component={MainScreen} />
              <Stack.Screen name="SetPw" component={SetPasswordScreen} />
              <Stack.Screen
                name="RemainEditor"
                component={RemainEditorScreen}
              />
              <Stack.Screen name="Album" component={AlbumScreen} />
              <Stack.Screen name="Setting" component={MypageScreen} />
            <Stack.Screen name="SetAlert" component={SetAlertScreen} />
            <Stack.Screen name="SetLock" component={SetLockScreen} />
            <Stack.Screen name="CheckPw" component={CheckPasswordScreen} />
            </Stack.Navigator>
          </NavigationContainer>
        </GestureHandlerRootView>
      </ToastProvider>
    </QueryClientProvider>
  );
};

export default App;

import {Switch, View} from 'react-native';
import Text from '../../components/CustomText';
import React, {useEffect, useState} from 'react';
import MypageModal from '../../components/mypage/MypageModal';
import {LOCK_ALERT_1, LOCK_ALERT_2} from '../../constants/mypage';
import Header from '../../components/Header';
import SettingItem from '../../components/mypage/MypageItem';
import InfoIcon from '../../assets/icon/information.svg';
import PasswordIcon from '../../assets/icon/password.svg';
import ChangeIcon from '../../assets/icon/change.svg';
import FaceLockIcon from '../../assets/icon/face-security.svg';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {StackNavigationProp} from '@react-navigation/stack';
import {RootStackParamList} from '../../../App';

interface SetLockScreenProps {
  navigation: StackNavigationProp<RootStackParamList>;
}

export default function SetLockScreen({navigation}: SetLockScreenProps) {
  const [usePassword, setUsePassword] = useState<boolean>(false);
  const [useBiodata, setUseBiodata] = useState<boolean>(false);
  const [password, setPassword] = useState<string>('');

  useEffect(() => {
    AsyncStorage.getItem('password').then(data => {
      if (data) setPassword(data);
    });
  }, []);

  return (
    <View className="flex-1 bg-primary-100">
      <Header pageName="설정 - 화면잠금" />
      {/* 설정 리스트뷰 */}
      <View className="flex-1 mt-4 gap-9">
        <View className="gap-5">
          <View className="px-4">
            <View className="flex-row items-center">
              <InfoIcon />
              <Text className="text-xl ml-2">{LOCK_ALERT_1}</Text>
            </View>
            <View className="flex-row">
              <View className="mt-2">
                <InfoIcon />
              </View>
              <Text className="text-xl ml-2">{LOCK_ALERT_2}</Text>
            </View>
          </View>
          <View>
            <SettingItem
              icon={<PasswordIcon />}
              title="비밀번호 설정"
              content={
                <Switch
                  trackColor={{true: '#444444', false: '#D9D9D9'}}
                  thumbColor={usePassword ? '#444444' : '#D9D9D9'}
                  ios_backgroundColor="#3e3e3e"
                  onValueChange={() => {
                    setUsePassword(!usePassword);
                  }}
                  value={usePassword}
                />
              }
              onPress={() => {}}
            />
            <SettingItem
              icon={<ChangeIcon />}
              disabled={!usePassword}
              title="비밀번호 변경"
              onPress={()=>{navigation.navigate('SetPw')}}
            />
          </View>
          <SettingItem
            icon={<FaceLockIcon />}
            title="생체 인증"
            content={
              <Switch
                trackColor={{true: '#444444', false: '#D9D9D9'}}
                thumbColor={useBiodata ? '#444444' : '#D9D9D9'}
                ios_backgroundColor="#3e3e3e"
                onValueChange={() => {
                  setUseBiodata(!useBiodata);
                }}
                value={useBiodata}
              />
            }
            onPress={() => {}}
          />
        </View>
      </View>
    </View>
  );
}

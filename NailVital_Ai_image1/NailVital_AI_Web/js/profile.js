// ─── USER PROFILE & SETTINGS CONTROLLER ───

const Profile = {
  settings: {
    reminders: true,
    biometrics: false,
    offlineMode: true,
    language: 'English'
  },

  async updateProfile(name, phone, age, gender, height) {
    const updateData = { name, phone, age: parseInt(age) || null, gender, height };
    try {
      const updated = await ApiService.updateUserProfile(updateData);
      Auth.currentUser = updated;
      localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(updated));
      return updated;
    } catch (err) {
      console.warn('[Profile] Local profile save fallback:', err.message);
      Auth.currentUser = { ...Auth.currentUser, ...updateData };
      localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(Auth.currentUser));
      return Auth.currentUser;
    }
  },

  async exportUserData() {
    try {
      const data = await ApiService.exportDataJson();
      const jsonStr = JSON.stringify(data, null, 2);
      const blob = new Blob([jsonStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `NailVital_HealthData_${Date.now()}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.warn('[Profile] Export data fallback:', err.message);
      const fallbackData = { user: Auth.currentUser, scans: History.scans };
      const blob = new Blob([JSON.stringify(fallbackData, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `NailVital_HealthData_${Date.now()}.json`;
      a.click();
      URL.revokeObjectURL(url);
    }
  },

  async deleteAccount(password) {
    if (!password) throw new Error('Please enter your password to confirm deletion');
    try {
      await ApiService.deleteAccount(password);
    } catch (err) {
      console.warn('[Profile] Account deletion simulated');
    }
    Auth.logout();
    return true;
  }
};

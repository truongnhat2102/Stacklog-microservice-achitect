const axios = require('axios');

// Hàm lấy thông tin các nhóm từ API class-service
async function getGroups(classId, token) {
  try {
    console.log(token);
    const response = await axios.get(`http://classservice:2003/group/class/${classId}`, {
      headers: {
        Authorization: `Bearer ${token}`  // Kẹp token vào header
      }
    });
    return response.data; // Trả về dữ liệu nhóm
  } catch (error) {
    console.error('Error fetching groups:', error);
    throw error;
  }
}

// Hàm lấy thông tin email của học viên từ API profile-service
async function getUserEmail(userId, token) {
  try {
    const response = await axios.get(`http://profileservice:2001/user/${userId}`, {
      headers: {
        Authorization: `Bearer ${token}`  // Kẹp token vào header
      }
    });
    return response.data.user.email; // Trả về email của người dùng
  } catch (error) {
    console.error(`Error fetching user email for ${userId}:`, error);
    throw error;
  }
}

// Hàm lấy tất cả các studentId từ API và trích xuất email của họ
async function getStudentEmails(listClassId, token) {
  try {
    // Lấy danh sách nhóm từ class-service, sử dụng Promise.all để đợi các promise
    const groups = await Promise.all(listClassId.map(classId => getGroups(classId, token)));
    console.log(groups);

    // Lấy danh sách userId của học viên từ các nhóm
    const userIds = [];
    groups[0]?.forEach(group => {
      // Kiểm tra xem group và group.groupStudents có tồn tại và không rỗng không
      if (group && group.groupStudents && group.groupStudents.length > 0) {
        group.groupStudents.forEach(student => {
          if (student && student.userId) {
            userIds.push(student.userId);
          }
        });
      }
    });

    // Lấy email của từng học viên
    const emails = [];
    for (const userId of userIds) {
      const email = await getUserEmail(userId, token); // Lấy email của từng học viên
      emails.push(email);
    }

    return emails;
  } catch (error) {
    console.error('Error getting student emails:', error);
    throw error;
  }
}



module.exports = {
  getGroups,
  getUserEmail,
  getStudentEmails
};
